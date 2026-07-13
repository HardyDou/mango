package io.mango.plugin.gen;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

/** Compatibility goal that blocks the retired, non-governed module generator. */
@Deprecated(forRemoval = true)
@Mojo(name = "gen-module", requiresProject = false)
public final class GenModuleMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException {
        throw new MojoExecutionException(
                "mango:gen-module is retired because it cannot enforce the published business"
                    + " template. Install @mango/cli and run: mango module add <module> --aggregate"
                    + " <aggregate> --aggregate-name <中文聚合名> --module-name <中文模块名> --project-dir"
                    + " <dir>");
    }
}
