cd "examples/example-aspects"
call mvn -Dmaven.test.skip=true -DskipTest=true -PautoInstallBundle clean install
cd "../../weaver"
call mvn -Dmaven.test.skip=true -DskipTest=true -PautoInstallPackage clean install
cd "../examples/example-target"
call mvn -Dmaven.test.skip=true -DskipTest=true -PautoInstallBundle clean install
cd ../../