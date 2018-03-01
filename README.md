velocity-cli
============

Command line interface to Apache Velocity.

## Usage

```bash
$ velocity -h
java VelocityCommandLine -c foo=bar -r /my/path/to/templates -t template.vm [-o output.txt] 

arguments:
   -a, --about  display about message [optional]
   -h, --help  display help message [optional]
   -c, --context [class java.lang.String]  context as comma-separated key value pairs [required]
   -r, --resourcePath [class java.io.File]  Resource Path [required]
   -t, --template [class java.io.File]  template file [required]
   -o, --output [class java.io.File]  output file, default stdout [optional]
   -e, --encoding [class java.lang.String]  encoding, default utf-8 [optional]
   -x, --escapetool [class java.lang.String]  add escapetool into context [optional]
   -p, --propertiesFile [class java.io.File]  add properties into context, default null or System.getProperties, if -P given [optional]
   -P, --propertiesName [class java.lang.String]  name for properties, default Properties [optional]
   -l, --logLevel [class java.lang.String]  log level, default SEVERE [optional]
```
