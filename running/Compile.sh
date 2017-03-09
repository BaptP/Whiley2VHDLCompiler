cd ..
mvn package
mv target/*.jar ../wdk-v0.4.0/lib/
cd running
./wdk-v0.4.0/bin/wy vhdlcompile example.whiley

#cp example.vhd ../FPGA/foo/src/