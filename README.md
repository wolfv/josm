# JOSM – the Java OpenStreetMap Editor

![Banner](/images_nodist/logo/bannerhorizontal.jpg)

# I. Install & Launch

## Installation notes

To run JOSM, you need:

* The JOSM .jar file, e.g., [josm-tested.jar](//josm.openstreetmap.de/josm-tested.jar) or [josm-latest.jar](//josm.openstreetmap.de/josm-latest.jar)
* [Java Runtime Environment (JRE) 7, or later](//java.com/download).


## How to get Java Runtime Environment

You need JRE Version 7, or later.

Microsoft Windows and Apple Mac OS X users should visit https://www.java.com
and download the latest Java executable for their system.

Linux users should visit http://www.oracle.com/technetwork/java/index.html
There is a Linux binary installer, which you must execute from a console, or
use the mechanism of your distribution's packaging system.


## How to launch
### Windows
Launch by double-clicking on the .jar file.
If this does not work, open a command shell and type the following in the directory that holds the file (Please replace josm-latest.jar with the name of your .jar file, if it is not named `josm-latest.jar`):
```shell
java -jar josm-latest.jar
```
### Linux
Open a shell, go to the file directory and type the following to launch:
```
java -jar josm-latest.jar
```
If this does not work, try to set your JAVA_HOME variable to the java executable location (the root location, not the bin).

### MacOS X
Just click on the .jar file icon.

# II. Development

## How to get the source code

### Git

This repository is not the only one mirroring https://josm.openstreetmap.de/svn/trunk. There is e.g. also an “official” (meaning published in the GitHub-organization named “openstreetmap”) git repository for JOSM on GitHub, that is automatically updated, so it might be more up to date than this repository here.

__Note:__ The two repositories are __not__ compatible in the sense that the SHA1-hashes of the git-commits are different for commits that mirror the very same SVN commit. So best choose one of the repositories and stick with it in the future, or you might face some problems (merge conflicts, …).

Here is a quick comparison of both repositories:

<table>
<tr><td></td><th><a href="https://github.com/floscher/josm">floscher/josm</a></th><th><a href="https://github.com/openstreetmap/josm">openstreetmap/josm</a></th></tr>
<tr><th>Imported versions</th><td>since version 1</td><td>since version 321</td></tr>
<tr><th>Imported branches</th><td>all (`trunk` and `0.5`)</td><td>`trunk`</td></tr>
<tr><th>Importer</th><td>git-svn</td><td>git-svn</td></tr>
<tr><th>Update mode</th><td>manual (irregularly)</td><td>automated (right after SVN has changed)</td></tr>
<tr><th>Marks stable releases?</th><td>yes, with git-tags and GitHub releases<br>(starting with version 15.2)</td><td>no</td></tr>
<tr><th>Handling of svn externals</th><td>Replaced by git submodules</td><td>Actual contents of external location inserted by mirroring bot</td></tr>
</table>

#### floscher/josm

Download the git repository via the command line:
```shell
git clone git@github.com:floscher/josm.git
```
This creates a new folder called `josm` inside the directory where you executed the command. This folder contains the full history of the git repository.

If you just need some of the latest revisions, use:
```shell
git clone --depth=N git@github.com:floscher/josm.git
```
where N is the number of revisions you want (e.g. 10 for the latest ten revisions).

#### openstreetmap/josm

For downloading from the official repo, use:
```shell
git clone git@github.com:openstreetmap/josm.git
```

### SVN

Download it directly from the subversion at
https://josm.openstreetmap.de/svn/trunk. To use the command line subversion
client, type

```shell
svn co https://josm.openstreetmap.de/svn/trunk josm
```

## Files & directories
This is an overview of the files and directories in the JOSM code repository:
```
- build.xml                 ant build file (standard way to create a JOSM binary)
- CONTRIBUTION              list of major code contributors
- data/                     data files that will be included in the JOSM jar file
    - fonts/                font files used for map rendering
    - projection/           projection files
      - *.gsb               NTv2 grid files for projection support
      - epsg                list of projection definitions
    - *.lang                translation data
    - *.xsd                 xml schema files for validation of configuration files
    - help-browser.css      CSS file for the help sites (HTML content is downloaded from the website
                            on demand, but displayed inside the programm in a Java web browser component.)
    - validator/            data files used by the JOSM validator feature
      - *.cfg               files designed for the old tagchecker, still used
      - *.mapcss            default validation rules for the new mapcss-based tagchecker
- data_nodist/              data files that are useful for development, but not distributed
    - exif-direction-example.jpg
                            sample image, that contains direction information in the EXIF header
                            (keys: Exif.GPSInfo.GPSImgDirectionRef, Exif.GPSInfo.GPSImgDirection)
    - filterTests.osm       used for unit testing of the filter feature
                            (see test/unit/org/openstreetmap/josm/data/osm/FilterTest.java)
    - Join_Areas_Tests.osm  some examples to test the 'join areas' feature
    - mapcss/               sample map styles and corresponding data files for regression testing
    - projection-reference-data.csv
                            reference data for projection tests
                            (see test/unit/org/openstreetmap/josm/data/projection/ProjectionRefTest.java)
    - projection-regression-test-data.csv
                            regression data for projection tests
                            (see test/unit/org/openstreetmap/josm/data/projection/ProjectionRegressionTest.java)
- geticons.pl               tool, to find all used icons and allows deleting unused icons
- gpl-2.0.txt, gpl-3.0.txt  full text of the GNU General Public License
- images/                   images distributed with the JOSM binary
    - icons                 images for the Potlatch 2 style
    - styles/standard       images for the main map style (external repository)
- images_nodist/            images, which are not for distribution, but may be useful later (e.g. high
                            resolution and vector versions)
- josm.jnlp                 Java Web Start launcher file (used on the website for the tested version)
- josm-latest.jnlp          Java Web Start launcher file (used on the website for the latest version)
- LICENSE                   the JOSM license terms
- linux/                    files useful for Linux distributions, including Appdata files, .desktop
                            files, Debian/Ubuntu scripts, man pages, icons, etc.
- macosx/                   files needed to create the MacOS X package
- netbeans/                 preconfigured Netbeans project
- optimize-images           short script to decrease size of PNG images
- patches/                  patches for external libraries used in JOSM (see below)
- README                    this file
- resources/                resource files that will be included in the JOSM jar file
- scripts/                  various scripts used by JOSM developers
- src/                      the source code of the program
- start.html                HTML page to run the applet version of JOSM
- styles/                   map styles included in JOSM
- sytles_nodist/            files needed for map style maintenance
    - potlatch2/README      infos on how to update the Potlatch 2 style from upstream sources
- test/                     automated software tests
    - data/                 resources used for some tests
    - functional/           functional tests (source code)
    - lib/                  libraries needed for (some of) the tests
    - performance/          performance tests (source code)
    - unit/                 unit tests (source code)
- tools/                    libraries and tools that help in the development process
    - animal-sniffer-ant-tasks-1.13.jar
                            used to build and check code signatures to ensure plugins binary compatibility
    - appbundler-1.0ea.jar  used to build Mac OS X package for Oracle Java 7
    - findbugs/             libs and config files for findbugs (automatically detects common bugs and potential
                            problems in source code); can be launched as an ant target in build.xml
    - groovy-all-2.3.9.jar  used for some unit tests and various scripts
    - jacocoant.jar         used to include coverage data into JUnit test reports
    - javacc.jar            used in the build process to generate some .java files from a javacc source file
                            (src/org/openstreetmap/josm/gui/mappaint/mapcss/MapCSSParser.jj)
    - proguard.jar          optimize final binary jar - see build.xml (not used in production so far)
    - xmltask.jar           used to edit XML files from Ant for the OSX package
- windows/                  files needed to create the Windows installer
```

## The `patches` directory

Some libraries that JOSM depends on, are patched for various reasons. The files in the patches directory can be used to roll back these customizations. This is useful in order to

 * inspect the changes
 * update to a newer version of the library but keep the modifications

You can use `quilt` to manage the patches. E.g. the following command applies all of them:

```shell
 $ quilt push -a
```

Of course, it is also possible to apply the patch files manually one by one.

## Third party libraries

There are some third party libraries which are directly included in the source code tree, in particular:

* __jmapviewer__: Java component to browse a TMS map<br>
    src/org/openstreetmap/gui (svn external)
    → http://svn.openstreetmap.org/applications/viewer/jmapviewer/
* __Apache commons codec__: Better Base64 support<br>
    src/org/apache/commons/codec (svn external)
    → http://svn.apache.org/repos/asf/commons/proper/codec/trunk/src/main/java/org/apache/commons/codec
* __Apache commons compress__: Support for bzip2 compression when opening files<br>
    src/org/apache/commons/compress/compressors (svn external)
    → http://svn.apache.org/repos/asf/commons/proper/compress/trunk/src/main/java/org/apache/commons/compress/compressors
* __Apache commons validator__: Improved validator routines<br>
    src/org/openstreetmap/josm/data/validation/routines
    → http://commons.apache.org/proper/commons-validator
* __SVG Salamander__: Support for SVG image format<br>
    src/com/kitfox/svg
    → https://svgsalamander.java.net/
* __Metadata Extractor__: Read EXIF Metadata of photos<br>
    src/com/drew
    → https://www.drewnoakes.com/code/exif/
* __Signpost__: OAuth library<br>
    src/oauth, src/com/google
    → https://code.google.com/p/oauth-signpost/
* __GNU getopt Java port__: Command line argument processing library<br>
    src/gnu/getopt
    → http://www.urbanophile.com/arenn/hacking/download.html
* __MultiSplitPane__: Small lib for GUI layout management<br>
    src/org/openstreetmap/josm/gui/MultiSplitLayout.java, MultiSplitPane.java
    → http://today.java.net/pub/a/today/2006/03/23/multi-split-pane.html
* __swinghelper__: Class CheckThreadViolationRepaintManager to find classpath violations<br>
    src/org/jdesktop/swinghelper/debug/CheckThreadViolationRepaintManager.java
    → https://java.net/projects/swinghelper
