# DJITello4J OpenCV 
An easy to use library to programmatically control your DJI Tello in Java, with OpenCV and FFMPEG.

*Only the normal DJI Tello, (non EDU edition) is supported. You can still access the EDU API using a custom command, but there is no support as of right now for Swarm control*

## OpenCV Usage 
Compile the gradle project and publish it to your local maven repository
`./gradlew build`
`./gradlew publishToMavenLocal`

Please keep in mind that due to the very large dependencies, this initial compile may take a while. 
 
 Now include the project in your `build.gradle`
 

    repositories {
	    mavenLocal()
    }
    dependencies {
	    implementation 'org.emeraldcraft.djitello4j_opencv:version'
    }
  
  > On Windows, when you connect to your Tello drone, you must mark its Wi-Fi network as a private network! 

 Example Code can be found under the `/examples/` folder.

## Camera Integration
Make sure that you call `Tello#streamon` before attempting to fetch frames from the camera. You can call this method multiple times. 
