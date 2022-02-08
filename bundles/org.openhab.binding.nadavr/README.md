# NAD Electronics - Audio/Video Receiver Binding

![NAD Electronics](doc/NAD_logo_red.png)

This binding integrates NAD Surround Sound Receivers/Amplifiers via **Telnet** using: <ul><li>an IP Ethernet connection on devices that have an Ethernet(LAN) port</li><li>or serial RS-232 interface using an IP to Serial converter (for example Global Cache's [WF2SL](https://www.globalcache.com/products/itach/wf2slspecs/), [IT2SL](https://www.globalcache.com/products/itach/ip2slspecs/), USR IOT's [USR-TCP232-302](https://www.pusr.com/products/1-port-rs232-to-ethernet-converters-usr-tcp232-302.html), etc)</li></ul>

Integration details can be found in the NAD Electronics command protocol documentation  here: [Protocol Integration Documentation](https://nadelectronics.com/software/#Protocol)

The binding has been tested with an NAD T-787 using: <ul><li>a direct Ethernet (LAN) connection on the receiver via Telnet(TCP)</li> <li> and with an [XM Direct Home Tuner](https://shop.siriusxm.com/support/xm-direct-home-tuner.html) connected to the T-787</li></ul>  

_DAB Tuner functionality has been included in the binding, but has not been tested._

## Supported Things

| Thing | Type | Description | Connection | Zones | Tested |
|:-:|:-:|:--|-------|:-:|:-:|
| ![T-187](doc/NAD-T-187.svg) | T187 | Connection to NAD T-187 Surround Sound Pre-Amplifier  | Ethernet | 4 |&#10060; No |
| ![T-758](doc/NAD-T-758-AV.svg) | T758 | Connection to NAD T-758 Surround Sound Receiver | Serial RS232 | 2 | &#10060; No |
| ![T-765](doc/NAD-T-765.svg) | T765 | Connection to NAD T-765 Surround Sound Receiver | Serial RS232 | 4 | &#10060; No |
| ![T-777](doc/NAD-T-777.svg) | T777 | Connection to NAD T-777 Surround Sound Receiver | Ethernet | 4 | &#10060; No |
| ![T-778](doc/NAD-T-778.svg) | T778 | Connection to NAD T-785 Surround Sound Receiver | Ethernet | 2 | &#10060; No |
| ![T-785](doc/NAD-T-785.svg) | T785 | Connection to NAD T-778 Surround Sound Receiver | Serial RS232 | 4 | &#10060; No |
| ![T-787](doc/NAD-T-787.svg) | T787 | Connection to NAD T-787 Surround Sound Receiver | Ethernet | 4 | &#9989; Yes |

## Discovery

The binding will auto-discover "support-things" (via mDNS) that are IP connected to the same network as the Open-Hab server.  

Auto discovered things will list the device details in the thing configuration "properties" section in the OpenHab UI.
<ul><li>Serial number (used to create unique thing UID)</li><li>The maximum number of zones the receiver supports</li><li>Model Id ("Type" in supported things)</li><li>Vendor</li></ul>


## Binding Configuration

_If your binding requires or supports general configuration settings, please create a folder ```cfg``` and place the configuration file ```<bindingId>.cfg``` inside it. In this section, you should link to this file and provide some information about the options. The file could e.g. look like:_

```
# Configuration for the NADAvr Binding
#
# Default secret key for the pairing of the NADAvr Thing.
# It has to be between 10-40 (alphanumeric) characters.
# This may be changed by the user for security reasons.
secret=openHABSecret
```

_Note that it is planned to generate some part of this based on the information that is available within ```src/main/resources/OH-INF/binding``` of your binding._

_If your binding does not offer any generic configurations, you can remove this section completely._

## Thing Configuration

The NAD AVR thing has the following configuration parameters:

| Parameter | Parameter Id | Req/Opt | Description | Default |  Type | Accepted Values |
| :--       | :--          | :-:               | :--         | :-:     | :-: | :--             |
| Zone Count of the Receiver | zoneCount | Required | User can configured number of zones to be configured | 2 | Integer | 1 - maxZones listed in Thing properties |  
| Host Name | hostname     | Required          | Host name assigned to device on the local network | | String | Host name or IPv4 address |
| IP Address | ipAddress   | Required          | The IPv4 address assigned the the NAD Receiver | | String | Any valid IPv4 address |
| Port      | telnetPort   | Required           | The network port for Telnet connection | 23 | Integer | Any valid TCP port number |
| Enable Preset Detail | enablePresetNames | Optional | User has provided an xml file listing details for tuner presets | false | Boolean | true or false |
| Preset Names File | presetNamesFilePath | Optional, <br />Required if enablePresetNames = true | File Name containing preset name details including path e.g. ```/etc/openhab/scripts/Preset_Names.xml``` | | String | Valid path and file name |

Since the NAD control protocol does not provide a means to retrieve the descriptive information for tuner presets, this binding provides the option to let the user create a file that can be used to give more meaning to the tuner preset channel.

**Note:** _If the NAD device does not have a tuner, then tuner preset name information will be ignored._

#### Preset Names XML file Example 
Tuner preset descriptions can be stored in an xml file that is then used to override the default (P01 - P40) options you're provided when selecting the tuner preset channel. 

**Steps to create file:**
<table>
<tr>
<th> Step </th> <th> Instructions </th>
</tr>
<tr>
<td> 01 </td>
<td> The template and schema files provided below should be used to simplify creating the preset names file. <ul> <li>Template <a href="doc/Preset_Names.xml">Preset_Names.xml</a> </li><li>Schema <a href="doc/NAD_Preset_Names.xsd">NAD_Preset_Names.xsd</a></li></ul>Follow the links to copy the source code. </td>
</tr>
<tr>
<td> 02 </td>
<td>Open an editor capable of working with and validating XML files like Visual Studio Code, Notepad++.</td>
</tr>
<tr>
<td> 03 </td>
<td>

Decide where you will store the two files you will create.  Recommend you save them in ```/etc/openhab/scripts/``` folder on your OpenHab system.
</td>
</tr>
<tr>
<td> 04 </td>
<td>

Open a new file, name it ```Preset_Names``` saving it with the .xml extension. <br> Open another new file, name it ```NAD_Preset_Names``` saving it with the .xsd extension

</td>
</tr>
<tr>
<td> 05 </td>
<td>

Click on the link for the Schema file, copy and paste the entire contents of the file into your ```NAD_Preset_Names.xsd``` file and save it.
</td>
</tr>
<tr>
<td> 06 </td>
<td>

Click on the link for the Template file, copy and paste the entire contents of the file into your ```Preset_Names.xml``` file and save it.
</td>
</tr>
<tr>
<td> 07 </td>
<td>

Edit the ```Preset_Names.xml``` file with your preset details, replacing the template examples with your details. <br> Depending on the editor, you may or may not have to define where to find the schema to validate your entries.
</td>
</tr>
<tr>
<td> 08 </td>
<td>
The basic XML format should have at the minimum, one or more presets (i.e. child elements) defined between the &lttunerPresets&gt root element.

```xml
<tunerPresets>
    <preset id="01">
        <band>FM</band>
        <frequency>105.70</frequency>
        <name>WAPL</name>
    </preset>
    <preset id="02">
        <band>AM</band>
        <frequency>1150</frequency>
        <name>WHBY</name>
    </preset>
    <preset id="03">
        <band>XM</band>
        <frequency>26</frequency>
        <name>Classic Vinyl</name>
    </preset>
</tunerPresets>
```

</td>
</tr>
<tr>
<td> 09 </td>
<td>XML validation will check for:
<table>
<tr>
<th> Element</th><th>Type</th><th> Validation Test </th>
</tr>
<tr>
<td>id</td><td>Integer</td><td> Two digits 01-40</td>
</tr>
<tr>
<td>band</td><td>String</td><td>FM, AM, XD, or DAB</td>
</tr>
<tr>
<td>frequency</td><td>String</td><td>1-6 characters long</td>
</tr>
<tr>
<td>name</td><td>String</td><td>1-20 characters long</td>
</tr>
</table>
</td>
</tr>
<tr>
<td>10</td><td>

Update the NAD Thing configuration with the preset name file including path e.g. ```/etc/openhab/scripts/Preset_Names.xml``` by clicking on the _Show advanced_ checkbox to display prompts for the tuner preset details.<ul><li>Toggle the Enable Preset Detail switch to On</li><li>Enter the preset name file including path in the Preset Name File prompt</li><li>Save the configuration; the file will be validated by the binding; and check for any error messages</li><li>If the file was found and is a valid format, the binding will be in an Online state</li></ul> 
</td>
</tr>
</table>


_Describe what is needed to manually configure a thing, either through the UI or via a thing-file. This should be mainly about its mandatory and optional configuration parameters. A short example entry for a thing file can help!_

_Note that it is planned to generate some part of this based on the XML files within ```src/main/resources/OH-INF/thing``` of your binding._

## Channels

_Here you should provide information about available channel types, what their meaning is and how they can be used._

_Note that it is planned to generate some part of this based on the XML files within ```src/main/resources/OH-INF/thing``` of your binding._

| channel  | type   | description                  |
|----------|--------|------------------------------|
| control  | Switch | This is the control channel  |

## Full Example

_Provide a full usage example based on textual configuration files (*.things, *.items, *.sitemap)._

## Any custom content here!

_Feel free to add additional sections for whatever you think should also be mentioned about your binding!_
