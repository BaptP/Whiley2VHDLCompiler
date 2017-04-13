package wyvc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import wycc.lang.Command;
import wycc.lang.Module;
import wycc.util.Logger;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyvc.commands.VHDLCompile;
import wyvc.io.TextualOutputStream;
import wyvc.io.VHDLFileWritter;
import wyvc.lang.VHDLFile;

public class Activator implements Module.Activator {

	public static final Content.Type<VHDLFile> ContentType = new Content.Type<VHDLFile>() {
		@SuppressWarnings({ "unchecked", "unused" })
		public Path.Entry<VHDLFile> accept(Path.Entry<?> e) {
			if (e.contentType() == this)
				return (Path.Entry<VHDLFile>) e;
			return null;
		}

		@Override
		public VHDLFile read(Path.Entry<VHDLFile> e, InputStream input) throws IOException {
			return null; // TODO Nécessaire ?
		}

		@Override
		public void write(OutputStream output, VHDLFile module)
				throws IOException {

			VHDLFileWritter writer = new VHDLFileWritter(new TextualOutputStream(output));
			writer.write(module);
		}

		@Override
		public String toString() {
			return "Content-Type: vhdl";
		}

		@Override
		public String getSuffix() {
			return "vhd";
		}
	};


	@Override
	public Module start(Module.Context context) {
		final Logger logger = new Logger.Default(System.err);
		final Command<?>[] commands = { new VHDLCompile(new wyc.Activator.Registry(), logger) };
		for (Command<?> c : commands)
			context.register(wycc.lang.Command.class, c);

		return null;
	}

	@Override
	public void stop(Module module, Module.Context context) {

	}

}
