package wyvc.builder;

import wyil.lang.Bytecode.Assign;
import wyil.lang.SyntaxTree;
import wyil.lang.SyntaxTree.Location;

public class Utils {
	public static void printLocation(Location<?> a, String n) {
		System.out.println(n+a.toString());
		for(Location<?> l : a.getOperands())
			printLocation(l, n+" |  ");
		if (a.getBytecode() instanceof Assign) {
			for(Location<?> l : a.getOperandGroup(SyntaxTree.LEFTHANDSIDE))
				printLocation(l, n+" |<-");
			for(Location<?> l : a.getOperandGroup(SyntaxTree.RIGHTHANDSIDE))
				printLocation(l, n+" |->");
		}
	}
}
