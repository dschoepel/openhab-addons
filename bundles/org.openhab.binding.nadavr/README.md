# NAD Electronics - Audio/Video Receiver Binding

![NAD Electronics](doc/NAD_logo_red.png)

This binding integrates NAD Srround Sound Receivers/Amplifiers via Telnet using: <ul><li>an IP Ethernet connection on devices that have an Ethernet(LAN) port</li><li>or serial RS-232 interface using an IP to Serial converter (for example USR IOT's [USR-TCP232-302](https://www.pusr.com/products/1-port-rs232-to-ethernet-converters-usr-tcp232-302.html), Global Cache's [WF2SL](https://www.globalcache.com/products/itach/wf2slspecs/), [IT2SL](https://www.globalcache.com/products/itach/ip2slspecs/), etc)</li></ul>

Integration details can be found in the NAD Electronics command protocol documentation  here: [Protocol Integration Documentation](https://nadelectronics.com/software/#Protocol)

The binding has been tested with an NAD T-787 using the direct Ethernet (LAN) connection on the device, as well as, via an IP to Serial converter (USR-TCP232-302).  

The binding was also tested with the [XM Direct Home Tuner](https://shop.siriusxm.com/support/xm-direct-home-tuner.html) connected to the T-787.  

_DAB Tuner functionality has been included in the binding, but has not been tested._

## Supported Things

| Thing  | Thing type   | description                  |
|----------|--------|-----------------------------------|
| ![T-187](doc\NAD-T-187.svg) | T187 | Connection to NAD T-187 Surround Sound Pre-Amplifier  |

_Please describe the different supported things / devices within this section._
_Which different types are supported, which models were tested etc.?_
_Note that it is planned to generate some part of this based on the XML files within ```src/main/resources/OH-INF/thing``` of your binding._

## Discovery

The binding will auto-discover "support-things" that are IP connected to the same network as the Open-Hab server  

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
