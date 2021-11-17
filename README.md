velocity-cli
============

Command line interface to Apache Velocity.

[![Maven Central](https://img.shields.io/maven-central/v/com.github.heuermh.velocity/velocity-cli.svg?maxAge=600)](http://search.maven.org/#search%7Cga%7C1%7Ccom.github.heuermh.velocity)
[![API Documentation](http://javadoc.io/badge/com.github.heuermh.velocity/velocity-cli.svg?color=brightgreen&label=javadoc)](http://javadoc.io/doc/com.github.heuermh.velocity/velocity-cli)

## Usage

```bash
$ velocity --help
usage:
velocity -t template.wm
  [-c foo=bar,baz=qux]
  [-r /resource/path]
  [-o output.txt]
  [-e euc-jp]
  [--verbose]

arguments:
   -a, --about  display about message [optional]
   -h, --help  display help message [optional]
   -t, --template [class java.io.File]  template file [required]
   -c, --context [class java.lang.String]  context as comma-separated key value pairs [optional]
   -r, --resource [class java.io.File]  resource path [optional]
   -o, --output [class java.io.File]  output file, default stdout [optional]
   -e, --encoding [class java.nio.charset.Charset]  encoding, default UTF-8 [optional]
   -v, --verbose  display verbose log messages [optional]
```
