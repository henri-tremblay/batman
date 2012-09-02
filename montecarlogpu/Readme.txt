##### Basic install #####
- Download the latest version of Aparapi (http://code.google.com/p/aparapi/downloads/list)
- Unzip it somewhere
- Set APARAPI_HOME variable to Aparapi directory
- Add APARAPI_HOME to your path

##### Compile #####
ant

##### Execute #####
montecarlo.[bat|sh]

you can specify there three parameters in order
first (com.amd.aparapi.executionMode): The execution mode. If empty, Aparapi will determine the best mode
second (size): the number of kernels that will run on parallel
third (iterations): the number of times the kernels will be called

the final number of iterations is size * iterations
   
##### Eclipse #####
- Add a variable named APARAPI_HOME pointing to your Aparapi directory
- Import the Eclipse project in Eclipse
