velocity-cli
============

Command line interface to Apache Velocity.

[![Build Status](https://travis-ci.org/heuermh/velocity-cli.svg?branch=master)](https://travis-ci.org/heuermh/velocity-cli)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.heuermh.velocity/velocity-cli.svg?maxAge=600)](http://search.maven.org/#search%7Cga%7C1%7Ccom.github.heuermh.velocity)
[![API Documentation](http://javadoc.io/badge/com.github.heuermh.velocity/velocity-cli.svg?color=brightgreen&label=javadoc)](http://javadoc.io/doc/com.github.heuermh.velocity/velocity-cli)

## Usage

```bash
$ velocity -h
usage:
velocity -c foo=bar -t template.wm [-o output.txt]

arguments:
   -a, --about  display about message [optional]
   -h, --help  display help message [optional]
   -c, --context [class java.lang.String]  context as comma-separated key value pairs [required]
   -t, --template [class java.io.File]  template file [required]
   -o, --output [class java.io.File]  output file, default stdout [optional]
```
