java \
   -Djava.library.path=../../com.amd.aparapi.jni\dist \
   -Dcom.amd.aparapi.executionMode=$1 \
   -Dsize=$2  \
   -Diterations=$3 \
   -classpath montecarlo.jar:$APARAPI_HOME/aparapi.jar \
   com.octo.montecarlo.Batman
