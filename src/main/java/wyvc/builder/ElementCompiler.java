package wyvc.builder;

import java.util.ArrayList;
import java.util.Collection;

import wyil.lang.Type;
import wyil.lang.WyilFile.FunctionOrMethod;
import wyvc.builder.VHDLCompileTask.VHDLCompilationException;
import wyvc.lang.Entity;
import wyvc.lang.Interface;
import wyvc.lang.LexicalElement.VHDLException;
import wyvc.lang.TypedValue.Port;
import wyvc.lang.TypedValue.Port.Mode;

public class ElementCompiler {
	public static Entity compileEntity(FunctionOrMethod function) throws VHDLException, VHDLCompilationException{
		Entity e =  new Entity(
			function.name(),
			compileInterface(function.name(), function.type())
		);
		ArchitectureCompiler ac = new ArchitectureCompiler(e);
		e.addArchitectures(ac.compile(function.getBody()));
		return e;
	}


	public static Interface compileInterface(String name, wyil.lang.Type.FunctionOrMethod type) {
		ArrayList<Port> ports = new ArrayList<Port>();
		int i = 0;
		for(Type t : type.params()){
			ports.add(new Port("I"+name+"_in_"+Integer.toString(i++), new wyvc.lang.Type.Signed(31,0), Mode.IN));
		}
		i = 0;
		for(Type t : type.returns()){
			ports.add(new Port("I"+name+"_out_"+Integer.toString(i++), new wyvc.lang.Type.Signed(31,0), Mode.OUT));
		}
		return new Interface(ports.toArray(new Port[0]));
	}

}
