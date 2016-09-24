package pan.cache.loader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Cache<T extends Serializable> {

	private final File location;
	private final String file;
	public static final File HOME_LOCATION = new File(System.getProperty("user.home")+File.separator);

	public Cache(File location, String file) {
		this.location = location;
		this.file = file;
	}
	
	public static File getFolderInHome(String folderName) {
		return new File(HOME_LOCATION, folderName+File.separator);
	}
	
	public boolean exists() {
		final File file = new File(location, this.file);
		return file.exists();
	}

	public void saveCache(final T data) {
		//SERIALIZE
		final File file = new File(location, this.file);
		if (!file.exists()) {
			file.getParentFile().mkdirs();
			try {
				file.createNewFile();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		try (
				ObjectOutput output
				= new ObjectOutputStream(
						new BufferedOutputStream(
								new FileOutputStream(file)));
				) {
			output.writeObject(data);
			output.flush();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public T loadCache() {
		final File file = new File(location, this.file);
		if (!file.exists())
			return null;
		//DESERIALIZE
		try (
				ObjectInput input
				= new ObjectInputStream(
						new BufferedInputStream(
								new FileInputStream(file)));
				) {
			return (T)input.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public T loadUncertainCache() throws IOException, ClassNotFoundException {
		final File file = new File(location, this.file);
		if (!file.exists())
			return null;
		//DESERIALIZE
		ObjectInput input = new ObjectInputStream(
				new BufferedInputStream(new FileInputStream(file))
				);
		T obj = (T)input.readObject();
		input.close();
		return obj;
	}

}
