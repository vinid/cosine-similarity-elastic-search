> Elastic Search plugin that makes possible the use of cosine similarity with TFIDF matrices
> link to the original script that I modified: https://github.com/imotov/elasticsearch-native-script-example

#How to install

> Download and compile the package

```sudo mvn package```

Stop elasticsearch before doing the next operations:

> install plugin using elasticsearch bin folder

``` sudo bin/plugin --url file:plugins/plugin/target/releases/example-plugin-1.0-SNAPSHOT.zip --install plugin ```

> to remove the plugin:

```sudo bin/plugin --remove plugin```

Example folder contains a file to test the code
