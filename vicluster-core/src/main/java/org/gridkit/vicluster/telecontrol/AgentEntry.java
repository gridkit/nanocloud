package org.gridkit.vicluster.telecontrol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class AgentEntry implements FileBlob {
    private static final String DIGEST_ALGO = "SHA-1";
    private final File file;
    private final String options;
    private String hash;

    public AgentEntry(File file, String options) {
        this.file = file;
        this.options = options;
    }

    @Override
    public File getLocalFile() {
        return file;
    }

    @Override
    public String getFileName() {
        return file.getName();
    }

    public String getOptions() {
        return options;
    }

    @Override
    public synchronized String getContentHash() {
        if (hash == null) {
            hash = StreamHelper.digest(StreamHelper.readFile(file), DIGEST_ALGO);
        }
        return hash;
    }

    @Override
    public InputStream getContent() {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public long size() {
        return StreamHelper.readFile(file).length;
    }

    @Override
    public String toString() {
        return "-javaagent:" + file.getAbsolutePath() + (options == null ? "" : "=" + options);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgentEntry)) return false;

        AgentEntry that = (AgentEntry) o;

        if (file != null ? !file.equals(that.file) : that.file != null) return false;
        return !(options != null ? !options.equals(that.options) : that.options != null);

    }

    @Override
    public int hashCode() {
        int result = file != null ? file.hashCode() : 0;
        result = 31 * result + (options != null ? options.hashCode() : 0);
        return result;
    }
}
