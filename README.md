# DJITello4J
An easy to use library to programmatically control your DJI Tello in Java.

*Only the normal DJI Tello, (non EDU edition) is supported. You can still access the EDU API using a custom command*

## Usage 
Compile the gradle project and publish it to your local maven repository
`./gradlew build`
`./gradlew publishToMavenLocal`
 
 Now include the project in your `build.gradle`
 

    repository {
	    mavenLocal()
    }
    dependencies {
	    implementation 'org.emeraldcraft.djitello4j:version'
    }
  
  > On Windows, when you connect to your Tello drone, you must mark its Wi-Fi network as a private network! 

 Example Code can be found under the `/examples/` folder.

## Camera Integration
This project uses FFmpeg and OpenCV for allowing you to be able to access the camera stream of the Tello, but these dependencies are massive (~100mb). 
Please use the [opencv](https://github.com/EmeraldWither/DJITello4J/tree/opencv_camera) branch to be able to compile a version with camera support. 
