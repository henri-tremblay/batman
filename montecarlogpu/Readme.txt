##### Basic install #####
- Download the latest version of Aparapi (http://code.google.com/p/aparapi/downloads/list)
- Unzip it somewhere
- Set APARAPI_HOME variable to Aparapi directory
- Add APARAPI_HOME to your path

##### Compile #####
ant

##### Execute #####
Linux:
java -cp montecarlo.jar:$APARAPI_HOME/aparapi.jar -Djava.library.path=$APARAPI_HOME -Dsize=256 -Diterations=20000 com.octo.montecarlo.Batman

Windows:
java -cp montecarlo.jar:%APARAPI_HOME%/aparapi.jar -Djava.library.path=%APARAPI_HOME% -Dsize=256 -Diterations=20000 com.octo.montecarlo.Batman

size: the number of kernels that will run on parallel
iterations: the number of times the kernels will be called

the final number of iterations is size * iterations
   
##### Eclipse #####
- Add a variable named APARAPI_HOME pointing to your Aparapi directory
- Import the Eclipse project in Eclipse
