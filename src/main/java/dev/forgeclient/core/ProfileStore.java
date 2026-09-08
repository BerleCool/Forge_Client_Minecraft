package dev.forgeclient.core;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/** Bounded local files, last-good backups and atomic replacement. Never follows file symlinks. */
public final class ProfileStore {
    private static final int MAX_BYTES=262144;
    private final Path root;
    public ProfileStore(Path root)throws IOException {
        this.root=root.toAbsolutePath().normalize();
        Files.createDirectories(this.root);
        for(Path p=this.root;p!=null;p=p.getParent())if(Files.isSymbolicLink(p))throw new IOException("Symbolic profile directory");
        if(!Files.isDirectory(this.root,LinkOption.NOFOLLOW_LINKS))throw new IOException("Not a profile directory");
    }
    public static boolean validName(String name){
        return name!=null&&name.matches("[a-z0-9_-]{1,32}")&&!name.matches("con|prn|aux|nul|com[1-9]|lpt[1-9]");
    }
    private Path file(String name)throws IOException{
        if(!validName(name))throw new IOException("Invalid profile name");
        Path p=root.resolve(name+".properties");safe(p);return p;
    }
    private void safe(Path p)throws IOException{
        if(Files.isSymbolicLink(root)||Files.isSymbolicLink(p))throw new IOException("Symbolic profile path refused");
        if(Files.exists(p,LinkOption.NOFOLLOW_LINKS)&&!Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS))throw new IOException("Not a regular profile file");
    }
    public synchronized boolean exists(String name){
        try{return Files.isRegularFile(file(name),LinkOption.NOFOLLOW_LINKS);}catch(IOException e){return false;}
    }
    public synchronized Properties load(String name)throws IOException{return read(file(name));}
    public synchronized Properties loadBackup(String name)throws IOException{return read(root.resolve(file(name).getFileName()+".bak"));}
    private Properties read(Path path)throws IOException{
        safe(path);
        if(Files.size(path)>MAX_BYTES)throw new IOException("Profile exceeds 256 KiB");
        byte[] bytes;
        // Bound the read as well as the initial stat in case another process changes the file.
        try(InputStream input=Files.newInputStream(path,LinkOption.NOFOLLOW_LINKS);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[4096];int n;
            while((n=input.read(buffer))!=-1){if(out.size()+n>MAX_BYTES)throw new IOException("Profile exceeds 256 KiB");out.write(buffer,0,n);}
            bytes=out.toByteArray();
        }
        Properties p=new Properties();
        try(Reader reader=new InputStreamReader(new ByteArrayInputStream(bytes),StandardCharsets.UTF_8)){p.load(reader);}
        catch(IllegalArgumentException e){throw new IOException("Malformed profile",e);}
        validate(p);return p;
    }
    private static void validate(Properties p)throws IOException{
        if(p==null||!"1".equals(p.getProperty("schema")))throw new IOException("Unsupported profile schema");
    }
    public synchronized void save(String name,Properties value)throws IOException{
        Path target=file(name),backup=root.resolve(target.getFileName()+".bak"),damaged=root.resolve(target.getFileName()+".damaged");
        safe(backup);safe(damaged);validate(value);
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(Writer writer=new OutputStreamWriter(out,StandardCharsets.UTF_8)){value.store(writer,"Forge Client local configuration");}
        byte[] bytes=out.toByteArray();if(bytes.length>MAX_BYTES)throw new IOException("Profile exceeds 256 KiB");
        // Backup is updated only from a valid prior primary; damaged data is preserved separately.
        if(Files.exists(target,LinkOption.NOFOLLOW_LINKS)){
            boolean valid=true;try{read(target);}catch(IOException e){valid=false;}
            if(valid)atomicWrite(backup,Files.readAllBytes(target));
            else {
                if(Files.size(target)>MAX_BYTES)throw new IOException("Refusing to replace oversized damaged profile");
                atomicWrite(damaged,Files.readAllBytes(target));
            }
        }
        atomicWrite(target,bytes);
    }
    private void atomicWrite(Path target,byte[] bytes)throws IOException{
        safe(target);Path temporary=Files.createTempFile(root,".forge-",".tmp");
        try{
            try(FileChannel channel=FileChannel.open(temporary,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS)){
                ByteBuffer data=ByteBuffer.wrap(bytes);while(data.hasRemaining())channel.write(data);channel.force(true);
            }
            safe(target);
            try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temporary);}
    }
    public synchronized List<String> names()throws IOException{
        List<String> names=new ArrayList<>();
        try(Stream<Path> entries=Files.list(root)){
            entries.filter(p->!Files.isSymbolicLink(p)&&Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)).forEach(p->{
                String n=p.getFileName().toString();if(n.endsWith(".properties")){String name=n.substring(0,n.length()-11);if(validName(name))names.add(name);}
            });
        }
        Collections.sort(names);return names;
    }
}
