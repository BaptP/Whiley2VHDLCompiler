package wyvc.builder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import wybs.lang.Build;
import wybs.lang.Build.Graph;
import wybs.lang.Build.Project;
import wyil.lang.WyilFile.FunctionOrMethod;
import wyvc.Activator;
import wycc.util.Logger;
import wycc.util.Pair;
import wyfs.lang.Path;
import wyfs.lang.Path.Entry;
import wyfs.lang.Path.Root;
import wyil.lang.WyilFile;
import wyvc.lang.Entity;
import wyvc.lang.VHDLFile;
import wyvc.builder.ControlFlowGraph.WyilSection;
import wyvc.builder.ElementCompiler.CompilationData;
import wyvc.io.GraphPrinter;

public class VHDLCompileTask implements Build.Task {
	private Logger logger = Logger.NULL;

	private Build.Project project;

	public VHDLCompileTask(Build.Project project) {
		this.project = project;
	}

	public Project project() {
		return project;
	}

	public Set<Entry<?>> build(Collection<Pair<Entry<?>, Root>> delta, Graph graph) throws IOException {
		Runtime runtime = Runtime.getRuntime();
		long start = System.currentTimeMillis();
		long memory = runtime.freeMemory();

		HashSet<Path.Entry<?>> generatedFiles = new HashSet<Path.Entry<?>>();


		for (Pair<Path.Entry<?>, Path.Root> p : delta) {
			Path.Root dst = p.second();
			System.out.println(p.toString());
			@SuppressWarnings("unchecked")
			Path.Entry<WyilFile> source = (Path.Entry<WyilFile>) p.first();
			WyilFile f = source.read();
			System.out.println(f.toString());

			ArrayList<Entity> entities = new ArrayList<Entity>();
			try {
				CompilationData data = new CompilationData();
				for (WyilFile.Type t  : f.types())
					data.types.put(t.name(), TypeCompiler.compileType(t.type(),data.types));
				for (FunctionOrMethod fct : f.functionOrMethods()){
					//Utils.printLocation(fct.getBody(), "");
//*/
					WyilSection s = ControlFlowGraphBuilder.buildGraph(fct, data.types);
					GraphPrinter.print(s.inputs, s.outputs, fct.name());
				}
/*/
					entities.add(ElementCompiler.compileEntity(fct, data));
				}

			} catch (VHDLException e) {
				e.printStackTrace();
				e.info();
//*/
			} catch (VHDLCompilationException e) {
				e.printStackTrace();
				e.info();
			}



			Path.Entry<VHDLFile> target = dst.create(source.id(), Activator.ContentType);
			graph.registerDerivation(source, target);
			generatedFiles.add(target);
			//*
			VHDLFile contents = new VHDLFile(entities.toArray(new Entity[0]));
			/*/
			VHDLFile contents = new VHDLFile();
			//*/
			target.write(contents);

		}
/*
		ArrayList<DataNode> roots = new ArrayList<>();
		ArrayList<DataNode> leaves = new ArrayList<>();
		DataNode a = new DataNode("var a", Collections.emptyList());
		roots.add(a);
		DataNode b = new DataNode("var b", Collections.emptyList());
		roots.add(b);
		DataNode c = new DataNode("var c", Collections.emptyList());
		roots.add(c);

		DataNode e = new DataNode("var e", Collections.singletonList(b));
		DataNode f = new DataNode("var f", Collections.singletonList(c));
		DataNode j = new DataNode("var j", Arrays.asList(e,f));

		leaves.add(new DataNode("var o", Arrays.asList(
			new DataNode("var h", Arrays.asList(
				new DataNode("var d", Arrays.asList(a,b)),
				e)),
			new DataNode("var m", Arrays.asList(
				new DataNode("var i", Collections.emptyList()),
				j)))));
		leaves.add(new DataNode("var p", Arrays.asList(
			j,
			new DataNode("var n", Arrays.asList(
				f,
				new DataNode("var l", Arrays.asList(
					new DataNode("var k", Collections.singletonList(e)),
					new DataNode("var g", Collections.singletonList(c)))))))));

		GraphPrinter.print(null, roots, leaves);*/

		long endTime = System.currentTimeMillis();
		logger.logTimedMessage("Wyil => VHDL: compiled " + delta.size() + " file(s)", endTime - start,
				memory - runtime.freeMemory());
		return generatedFiles;
	}


	public abstract static class VHDLCompilationException extends Exception {
		private static final long serialVersionUID = 1062123869833614980L;

		protected abstract void details();

		public void info() {
			System.err.println("Unsupported VHDL compilation feature");
			details();
		}

	}
}
