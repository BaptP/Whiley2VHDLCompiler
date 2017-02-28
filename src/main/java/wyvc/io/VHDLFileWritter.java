package wyvc.io;

import java.io.IOException;

import wyvc.io.Tokenisable.Token.StartToken;
import wyvc.lang.VHDLFile;

public class VHDLFileWritter {
	protected final TextualOutputStream output;

	public VHDLFileWritter(TextualOutputStream output){
		this.output = output;
	}

	public void write(VHDLFile vfile) throws IOException{
		StartToken f = new StartToken();
		vfile.addTokens(f);
		f.write(output);
	}


}
