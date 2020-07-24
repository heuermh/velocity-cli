velocity-cli
============

Command line interface to Apache Velocity.

[![Build Status](https://travis-ci.org/heuermh/velocity-cli.svg?branch=master)](https://travis-ci.org/heuermh/velocity-cli)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.heuermh.velocity/velocity-cli.svg?maxAge=600)](http://search.maven.org/#search%7Cga%7C1%7Ccom.github.heuermh.velocity)
[![API Documentation](http://javadoc.io/badge/com.github.heuermh.velocity/velocity-cli.svg?color=brightgreen&label=javadoc)](http://javadoc.io/doc/com.github.heuermh.velocity/velocity-cli)

## Usage

```bash
$ velocity --help
usage:
velocity -c foo=bar,baz=qux | -j context.json -r /resource/path -t template.wm [-o output.txt] [-e euc-jp] [--verbose]

arguments:
   -a, --about  display about message [optional]
   -h, --help  display help message [optional]
   -c, --context [class java.lang.String]  context as comma-separated key value pairs [optional]
   -j, --jsonFile [class java.io.File]  context as json file [optional]
   -r, --resource [class java.io.File]  resource path [optional]
   -t, --template [class java.io.File]  template file [required]
   -o, --output [class java.io.File]  output file, default stdout [optional]
   -e, --encoding [class java.nio.charset.Charset]  encoding, default UTF-8 [optional]
   -v, --verbose  display verbose log messages [optional]
```

Velocity takes the substitution values from a (https://velocity.apache.org/engine/2.0/apidocs/org/apache/velocity/VelocityContext.html)[VelocityContext].

`velocity` will construct this (https://velocity.apache.org/engine/2.0/apidocs/org/apache/velocity/VelocityContext.html)[VelocityContext] from the commandline, with either the `-c` or `-j` parameter. 

`-c` is fine for simple (flat) substitutions, for example `-c foo=bar,baz=qux`.

`-j` needs to be used for templates with nested variables, like `$something.other`. `-j` will read a JSON file. The json file should contain a dictionary
with the expected values, for example:
```
{
   "something": {
      "other": "also nice"
   }
}
```



