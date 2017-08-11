package wyvc.commands;

import java.io.OutputStream;

import wybs.util.StdBuildRule;
import wybs.util.StdProject;
import wyc.commands.Compile;
import wycc.util.Logger;
import wyfs.lang.Content;
import wyfs.lang.Content.Registry;
import wyil.lang.WyilFile;
import wyvc.builder.VHDLCompileTask;


public class VHDLCompile extends Compile {
	public VHDLCompile(Registry registry, Logger logger) {
		super(registry, logger);
	}
	public VHDLCompile(Content.Registry registry, Logger logger, OutputStream sysout, OutputStream syserr) {
		super(registry, logger, sysout, syserr);
	}

	@Override
	public String getName() {
		return "vhdlcompile";
	}

	@Override
	public String getDescription() {
		return "Compile Whiley files to VHDL";
	}

	@Override
	protected void addCompilationBuildRules(StdProject project) {
		super.addCompilationBuildRules(project);
		addWyil2vhdlBuildRule(project);
	}
	private void addWyil2vhdlBuildRule(StdProject project) {
		Content.Filter<WyilFile> wyilIncludes = Content.filter("**", WyilFile.ContentType);
		Content.Filter<WyilFile> wyilExcludes = null;
		VHDLCompileTask vhdlBuilder = new VHDLCompileTask(project);
		//if(verbose)
		//	jvmBuilder.setLogger(logger);
		project.add(new StdBuildRule(vhdlBuilder, wyildir, wyilIncludes, wyilExcludes, wyildir));
	}
}
