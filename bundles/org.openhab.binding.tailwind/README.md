# TailWind Binding - Smart Garage Door Controller

![TailWind logo](doc/Tailwind-logo-250x88-dark.png)

This binding is used to enable communication between OpenHab and [TailWind's Smart Automatic Garage Controller](https://gotailwind.com/).


## Overview

The garage door controllers are automatically discovered.  

There is a single Thing created for each controller connected to the local Ethernet-WiFi network with channels that allow control of the doors (garage, gates, doors...) connected to the controller.

OpenHab linked channels will be updated regardless of whether the Tailwind's web/smarphone app, door remote or other integrations are used to configure, open or close doors.

Details for connecting to the controller's API server can be found on TailWind's GitHub site here: [Tailwind Local Control API](https://github.com/Scott--R/Tailwind_Local_Control_API)

## Testing

#### TailWind iQ3: 
 
<ul>
<li>using auto discovery and adding thing via inbox (<b>preferred</b>)</li>
<li>using manual configuration</li>
<li>using file based setup </li>
<li>via a direct Ethernet (LAN) connection with the iQ3 on same local network as the OpenHab server</li> 
</ul>  

## Supported Things


| Thing | Type | Description | Connection | Doors | Tested |
|:-:|:-:|:--|-------|:-:|:-:|
| ![TailWind](doc/tailwind_iQ34.png) | iQ3 | Smart Automatic Garage Controller | Ethernet / WiFi | 3 | &#9989; Yes |

A typical Thing UID will have three components **bindingId**_ + **model** + **unique id** (the MAC address of the device). 

For example - **tailwind:iQ3:08d1f91202ec**

## Discovery

The Tailwind binding discovers the garage controller on the **local** network and creates an inbox entry for each discovered device. 

The binding can auto-discover the TailWind garage controllers present on your **local** network. Auto-discovery is enabled by default. To disable it, you can create a file in the services directory called tailwind.cfg with the following content:

```ruby
# Configuration for the nadavr binding
# 
# Auto discovery parameter 
# true to enable, false to disable  
org.openhab.tailwind:enableAutoDiscovery=false
```
This configuration parameter only controls the TailWind auto-discovery process, not the openHAB auto-discovery. Moreover, if the openHAB auto-discovery is disabled, the TailWind auto-discovery is disabled too.

Once added as a thing, the user can control up to three doors per controller; similarly to how they are controlled using TailWind's web or smartphone app.



## Thing Configuration

_Describe what is needed to manually configure a thing, either through the UI or via a thing-file._
_This should be mainly about its mandatory and optional configuration parameters._

_Note that it is planned to generate some part of this based on the XML files within ```src/main/resources/OH-INF/thing``` of your binding._

### `sample` Thing Configuration

The TailWind controller thing has the following configuration parameters:

| Parameter | Parameter Id | Req/Opt | Description | Default | Type | Accepted Values |
| :--  | :-- | :-: | :-- | :-: | :-: | :-- |
| Refresh Interval\* | refreshInterval | Optional | The refresh interval in **seconds** for polling the receiver settings (0=disabled) to update item details. | 0 | Integer | 0 = disabled, Greater Than 0 = enabled |

>Note: <b>*</b> items are hidden unless "Show advanced" checked on UI 

## Channels

_Here you should provide information about available channel types, what their meaning is and how they can be used._

_Note that it is planned to generate some part of this based on the XML files within ```src/main/resources/OH-INF/thing``` of your binding._

| Channel | Type   | Read/Write | Description                 |
|---------|--------|------------|-----------------------------|
| control | Switch | RW         | This is the control channel |

## Full Example

_Provide a full usage example based on textual configuration files._
_*.things, *.items examples are mandatory as textual configuration is well used by many users._
_*.sitemap examples are optional._

### Thing Configuration

```java
Example thing configuration goes here.
```

### Item Configuration

```java
Example item configuration goes here.
```

### Sitemap Configuration

```perl
Optional Sitemap configuration goes here.
Remove this section, if not needed.
```

## Any custom content here!

_Feel free to add additional sections for whatever you think should also be mentioned about your binding!_
