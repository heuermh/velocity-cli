velocity-cli
============

Command line interface to Apache Velocity.

## Usage

```bash
$ velocity -h
usage:
java VelocityCommandLine -c foo=bar -t template.wm [-o output.txt]

arguments:
   -a, --about  display about message [optional]
   -h, --help  display help message [optional]
   -c, --context [class java.lang.String]  context as comma-separated key value pairs [required]
   -t, --template [class java.io.File]  template file [required]
   -o, --output [class java.io.File]  output file, default stdout [optional]
```
