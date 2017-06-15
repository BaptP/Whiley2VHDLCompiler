cd ..
mvn package
mv target/*.jar running/wdk-v0.4.0/lib/
cd running
chmod +x ./wdk-v0.4.0/bin/wy
./wdk-v0.4.0/bin/wy vhdlcompile example.whiley

#cp example.vhd ../FPGA/foo/src/
