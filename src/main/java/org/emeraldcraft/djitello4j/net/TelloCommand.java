package org.emeraldcraft.djitello4j.Tello.net;

/**
 * @param command The command to send to the Tello.
 * @param response The response to the command. Can be null.
 * @param timeout The timeout in milliseconds. -1 for no timeout (not suggested) 
 */
public record TelloCommand(String command, String response, int timeout) {

}
