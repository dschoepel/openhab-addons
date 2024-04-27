package org.openhab.binding.tailwind.internal.Utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.text.Utilities;

import org.openhab.binding.tailwind.internal.TailwindModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utilites {

    private Logger logger = LoggerFactory.getLogger(Utilities.class);

    /**
     * Method to retrieve the maximum door count for the model to be used
     * in the thing properties and validating the thing configuration settings.
     *
     * @param model - validated from the mDNS discovery
     * @return maxDoors for the device
     */
    public int GetMaxDoors(String model) {
        int maxDoors = 1;
        for (TailwindModel supportedModel : TailwindModel.values()) {
            if (supportedModel.getId().equals(model)) {
                maxDoors = supportedModel.getMaxDoors();
            }
        }
        return maxDoors;
    }

    /**
     * Method to make first letter of a string upper case
     *
     * @param name
     * @return Converted string
     */
    public String makeFirstLetterUpperCase(String name) {
        String str1 = name.substring(0, 1).toUpperCase();
        String str2 = name.substring(1);
        String result = str1 + str2;
        return result;
    }

    /**
     * Retrieve the IP address for the OpenHab Host Server to be used
     * by the TailWind device's UDP client to send UDP status updates
     *
     * @return IP Address of the OpenHab host server
     * @throws UnknownHostException - host not found
     */
    public String getOHServerIP() throws UnknownHostException {
        InetAddress address1 = InetAddress.getLocalHost();
        // String hostName = address1.getHostName();
        String hostAddress = address1.getHostAddress();
        return hostAddress;
    }

    /**
     * Remove training dot in server url it it has one...
     *
     * @return Clean server url
     */
    public String getServerURL(String server) {
        // Remove last period from server name if it is there, 0 based index
        int lastDotPosition = server.lastIndexOf(".");
        if (server.length() == lastDotPosition + 1) {
            return server.substring(0, lastDotPosition);
        } else {
            return server;
        }
    }
}
