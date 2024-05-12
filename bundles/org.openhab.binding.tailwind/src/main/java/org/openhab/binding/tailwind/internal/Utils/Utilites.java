package org.openhab.binding.tailwind.internal.Utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.text.Utilities;

import org.openhab.binding.tailwind.internal.TailwindModel;
import org.openhab.core.thing.Thing;
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

    public String getServerURL(InetAddress address) {
        String serverURL = address.getHostName();
        return serverURL;
    }

    /**
     * Method to convert a float value in seconds to milliseconds and return
     * as a string value.
     *
     * @param seconds - value in seconds
     * @return - seconds converted to milliseconds as a string value
     */
    public long getSecondsToMilliseconds(float seconds) {
        return (long) (seconds * 1000);
    }

    /**
     * Method to build a meaningful thread name to use for scheduled jobs. Uses the thing UID and
     * configuration values to construct a unique name.
     *
     * @param thing - Use Uid to build name. Assume 3 parts (1=Binding, 2=Model, 3=unique Id)
     * @return threadName in format "binding+model+UID+"UDP-Rcvr:"
     */
    public String getThreadName(Thing thing) {
        // split the thing UID into parts then add back together
        String[] thingUid = thing.getUID().toString().split(":");
        String threadName = "";
        for (int i = 0; i <= (thingUid.length - 1); i++) {
            threadName = threadName.concat(thingUid[i]).concat("-");
        }
        threadName = threadName.concat("UDP-Rcvr:");
        ;
        return threadName;
    }

    public static boolean isValidIPAddress(final String ip) {
        String PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";

        return ip.matches(PATTERN);
    }

    /**
     * @param deviceId in format "_8_d1_f9_12_2_ec_" contains the MAC address each of 6 pairs separated by an underscore
     * @return macAddress re-formated to 12 digits and no separator
     */
    public String convertDeviceIdToMac(String deviceId) {

        String macAddress = "";
        String[] deviceIdArray = deviceId.split("_");
        for (int i = 0; i <= (deviceIdArray.length - 1); i++) {
            if (deviceIdArray[i].length() == 1) {
                macAddress += "0".concat(deviceIdArray[i]); // append 0
            } else {
                macAddress += deviceIdArray[i];
            }
        }
        return macAddress;
    }
}
