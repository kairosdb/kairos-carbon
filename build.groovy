import org.freecompany.redline.Builder
import org.freecompany.redline.header.Architecture
import org.freecompany.redline.header.Os
import org.freecompany.redline.header.RpmType
import org.freecompany.redline.payload.Directive
//import org.vafer.jdeb.DebMaker
//import org.vafer.jdeb.StdOutConsole
import tablesaw.AbstractFileSet
import tablesaw.RegExFileSet
import tablesaw.SimpleFileSet
import tablesaw.Tablesaw
import tablesaw.TablesawException
import tablesaw.addons.GZipRule
import tablesaw.addons.TarRule
import tablesaw.addons.ivy.IvyAddon
import tablesaw.addons.ivy.PomRule
import tablesaw.addons.java.Classpath
import tablesaw.addons.java.JarRule
import tablesaw.addons.java.JavaCRule
import tablesaw.addons.java.JavaProgram
import tablesaw.addons.junit.JUnitRule
import tablesaw.definitions.Definition
import tablesaw.rules.DirectoryRule
import tablesaw.rules.Rule
import tablesaw.rules.SimpleRule
import tablesaw.rules.CopyRule

import javax.swing.*

println("===============================================");

saw.setProperty(Tablesaw.PROP_MULTI_THREAD_OUTPUT, Tablesaw.PROP_VALUE_ON)

programName = "kairos-carbon"
//Do not use '-' in version string, it breaks rpm uninstall.
version = "1.4"
kairos_version = "1.3.0"
release = "1" //package release number
summary = "Kairos-carbon"
description = """\
Plugin for KairosDB ($kairos_version) that provides Carbon protocol listeners and a client
for forwarding data points on to Carbon servers
"""

saw.setProperty(JavaProgram.PROGRAM_NAME_PROPERTY, programName)
saw.setProperty(JavaProgram.PROGRAM_DESCRIPTION_PROPERTY, description)
saw.setProperty(JavaProgram.PROGRAM_VERSION_PROPERTY, version)
saw.setProperty(PomRule.GROUP_ID_PROPERTY, "org.kairosdb")
saw.setProperty(PomRule.URL_PROPERTY, "http://kairosdb.org")

saw = Tablesaw.getCurrentTablesaw()
saw.includeDefinitionFile("definitions.xml")

ivyConfig = ["provided", "test"]

rpmDir = "build/rpm"
rpmNoDepDir = "build/rpm-nodep"
new DirectoryRule("build")
rpmDirRule = new DirectoryRule(rpmDir)
rpmNoDepDirRule = new DirectoryRule(rpmNoDepDir)

//------------------------------------------------------------------------------
//Setup java rules
ivy = new IvyAddon()
		.addSettingsFile("ivysettings.xml")
		.setup()

buildLibraries = new RegExFileSet("lib", ".*\\.jar").recurse()
		.addExcludeDir("integration")
		.getFullFilePaths()

jp = new JavaProgram()
		//.setLibraryJars(buildLibraries)
		.setup()

jc = jp.getCompileRule()
jc.addDepend(ivy.getResolveRule("provided"))

jc.getDefinition().set("target", "1.8")
jc.getDefinition().set("source", "1.8")

jp.getJarRule().addFiles("src/main/resources", "kairos-carbon.conf")


pomRule = ivy.createPomRule("build/jar/pom.xml", ivy.getResolveRule("default"))
		.addDepend(jp.getJarRule())
		.addLicense("The Apache Software License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt", "repo")
		.addDeveloper("brianhks", "Brian", "brianhks1+kairos@gmail.com")
		.addDeveloper("jeff", "Jeff", "jeff.sabin+kairos@gmail.com")

//------------------------------------------------------------------------------
//==-- Maven Artifacts --==
mavenArtifactsRule = new SimpleRule("maven-artifacts").setDescription("Create maven artifacts for maven central")
		.addSource(jp.getJarRule().getTarget())
		.addSource(jp.getJavaDocJarRule().getTarget())
		.addSource(jp.getSourceJarRule().getTarget())
		.addSource("build/jar/pom.xml")
		.setMakeAction("signArtifacts")

void signArtifacts(Rule rule)
{
	for (String source : rule.getSources())
	{
		cmd = "gpg -ab "+source
		saw.exec(cmd)
	}
}

new JarRule("maven-bundle", "build/bundle.jar").setDescription("Create bundle for uploading to maven central")
		.addDepend(mavenArtifactsRule)
		.addFileSet(new RegExFileSet(saw.getProperty(JavaProgram.JAR_DIRECTORY_PROPERTY), ".*"))

//------------------------------------------------------------------------------
//Set information in the manifest file
manifest = jp.getJarRule().getManifest().getMainAttributes()
manifest.putValue("Manifest-Version", "1.0")
manifest.putValue("Tablesaw-Version", saw.getVersion())
manifest.putValue("Created-By", saw.getProperty("java.vm.version")+" ("+
			saw.getProperty("java.vm.vendor")+")")
manifest.putValue("Built-By", saw.getProperty("user.name"))
buildDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")
manifest.putValue("Build-Date", buildDateFormat.format(new Date()))

buildNumberFormat = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
buildNumber = buildNumberFormat.format(new Date())
manifest.putValue("Implementation-Title", "Kairos-Carbon")
manifest.putValue("Implementation-Vendor", "")
manifest.putValue("Implementation-Version", "${version}.${buildNumber}")

//Add git revision information
gitRevisionFile= ".gitrevision"
new File(gitRevisionFile).text = ""
ret = saw.exec(null, "git rev-parse HEAD", false, null, gitRevisionFile);
revision = new File(gitRevisionFile).text.trim()
new File(gitRevisionFile).delete()
if (ret == 0)
	manifest.putValue("Git-Revision", revision);


//------------------------------------------------------------------------------
//Setup unit tests
testClasspath = new Classpath(jp.getLibraryJars())
testClasspath.addPath(jp.getJarRule().getTarget())


testSources = new RegExFileSet("src/test/java", ".*Test\\.java").recurse()
		.getFilePaths()
testCompileRule = jp.getTestCompileRule()
testCompileRule.addDepend(ivy.getResolveRule("test"))

junitClasspath = new Classpath(testCompileRule.getClasspath())
junitClasspath.addPaths(testClasspath)
junitClasspath.addPath("src/main/java")
junitClasspath.addPath("src/test/resources")
junitClasspath.addPath("src/main/resources")

junit = new JUnitRule("junit-test").addSources(testSources)
		.setClasspath(junitClasspath)
		.addDepends(testCompileRule)

if (saw.getProperty("jacoco", "false").equals("true"))
	junit.addJvmArgument("-javaagent:lib_test/jacocoagent.jar=destfile=build/jacoco.exec")


if (saw.getProperty("jacoco", "false").equals("true"))
	junitAll.addJvmArgument("-javaagent:lib_test/jacocoagent.jar=destfile=build/jacoco.exec")

//------------------------------------------------------------------------------
//Build zip deployable application
rpmFile = "$programName-$version-${release}.rpm"
srcRpmFile = "$programName-$version-${release}.src.rpm"
ivyFileSet = new SimpleFileSet()

//Resolve dependencies for package
ivyResolve = ivy.getResolveRule("default")
resolveIvyFileSetRule = new SimpleRule()
		.addDepend(ivyResolve)
		.setMakeAction("doIvyResolve")

def doIvyResolve(Rule rule)
{
	classpath = ivyResolve.getClasspath()

	for (String jar in classpath.getPaths())
	{
		file = new File(jar)
		ivyFileSet.addFile(file.getParent(), file.getName())
	}
}

libFileSets = [
		new RegExFileSet("build/jar", ".*\\.jar"),
		//new RegExFileSet("lib", ".*\\.jar"),
		ivyFileSet
	]

scriptsFileSet = new RegExFileSet("src/scripts", ".*").addExcludeFile("kairosdb-env.sh")
webrootFileSet = new RegExFileSet("webroot", ".*").recurse()

zipLibDir = "kairosdb/lib"
zipConfDir = "kairosdb/conf"
tarRule = new TarRule("build/${programName}-${version}.tar")
		.addDepend(jp.getJarRule())
		.addDepend(resolveIvyFileSetRule)
		.addFileTo(zipConfDir, "src/main/resources", "kairos-carbon.conf")

for (AbstractFileSet fs in libFileSets)
	tarRule.addFileSetTo(zipLibDir, fs)


gzipRule = new GZipRule("package").setSource(tarRule.getTarget())
		.setDescription("Create deployable tar file")
		.setTarget("build/${programName}-${version}.tar.gz")
		.addDepend(tarRule)

//------------------------------------------------------------------------------
//Build rpm file
rpmBaseInstallDir = "/opt/kairosdb"
rpmRule = new SimpleRule("package-rpm").setDescription("Build RPM Package")
		.addDepend(jp.getJarRule())
		.addDepend(resolveIvyFileSetRule)
		.addDepend(rpmDirRule)
		.addTarget("$rpmDir/$rpmFile")
		.setMakeAction("doRPM")


def doRPM(Rule rule)
{
	//Build rpm using redline rpm library
	host = InetAddress.getLocalHost().getHostName()
	rpmBuilder = new Builder()
	rpmBuilder.with
			{
				description = description
				group = "System Environment/Daemons"
				license = "license"
				setPackage(programName, version, release)
				setPlatform(Architecture.NOARCH, Os.LINUX)
				summary = summary
				type = RpmType.BINARY
				url = "http://kairosdb.org"
				vendor = "KairosDB"
				provides = programName
				buildHost = host
				sourceRpm = srcRpmFile
			}

	rpmBuilder.addDependencyMore("kairosdb", kairos_version)

	//rpmBuilder.setPostInstallScript(new File("src/scripts/install/post_install.sh"))
	//rpmBuilder.setPreUninstallScript(new File("src/scripts/install/pre_uninstall.sh"))

	for (AbstractFileSet fs in libFileSets)
		addFileSetToRPM(rpmBuilder, "$rpmBaseInstallDir/lib", fs)


	rpmBuilder.addFile("$rpmBaseInstallDir/conf/kairos-carbon.conf",
			new File("src/main/resources/kairos-carbon.conf"), 0644, new Directive(Directive.RPMFILE_CONFIG | Directive.RPMFILE_NOREPLACE))

	println("Building RPM "+rule.getTarget())
	outputFile = new FileOutputStream(rule.getTarget())
	rpmBuilder.build(outputFile.channel)
	outputFile.close()
}

def addFileSetToRPM(Builder builder, String destination, AbstractFileSet files)
{
	for (String filePath : files.getFullFilePaths())
	{
		File f = new File(filePath)
		if (f.getName().endsWith(".sh"))
			builder.addFile(destination + "/" +f.getName(), f, 0755)
		else
			builder.addFile(destination + "/" + f.getName(), f)
	}
}


//------------------------------------------------------------------------------
//Build deb file
debRule = new SimpleRule("package-deb").setDescription("Build Deb Package")
		.addDepend(rpmRule)
		.setMakeAction("doDeb")

def doDeb(Rule rule)
{
	//Prompt the user for the sudo password
	//TODO: package using jdeb
	def jpf = new JPasswordField()
	def resp = JOptionPane.showConfirmDialog(null,
			jpf, "Enter sudo password:",
			JOptionPane.OK_CANCEL_OPTION)

	if (resp == 0)
	{
		def password = jpf.getPassword()
		sudo = saw.createAsyncProcess(rpmDir, "sudo -S alien --bump=0 --to-deb $rpmFile")
		sudo.run()
		//pass the password to the process on stdin
		sudo.sendMessage("$password\n")
		sudo.waitForProcess()
		if (sudo.getExitCode() != 0)
			throw new TablesawException("Unable to run alien application")
	}
}

installDir = saw.getProperty("kairos_home")

deployConfig = new CopyRule()
		.addFile("src/main/resources/kairos-carbon.conf")
		.setDestination(installDir + "/conf")

deployRule = new CopyRule("deploy").setDescription("Deploy to karios install")
		.addDepend(resolveIvyFileSetRule)
		.addDepend(jp.getJarRule())
		.addDepend(deployConfig)
		.setDestination(installDir + "/lib")

for (AbstractFileSet fs in libFileSets)
	deployRule.addFileSet(fs)


saw.setDefaultTarget("jar")


//------------------------------------------------------------------------------
//Build notification
def printMessage(String title, String message) {
	osName = saw.getProperty("os.name")

	Definition notifyDef
	if (osName.startsWith("Linux"))
	{
		notifyDef = saw.getDefinition("linux-notify")
	}
	else if (osName.startsWith("Mac"))
	{
		notifyDef = saw.getDefinition("mac-notify")
	}

	if (notifyDef != null)
	{
		notifyDef.set("title", title)
		notifyDef.set("message", message)
		saw.exec(notifyDef.getCommand())
	}
}

def buildFailure(Exception e)
{
	printMessage("Build Failure", e.getMessage())
}

def buildSuccess(String target)
{
	printMessage("Build Success", target)
}
