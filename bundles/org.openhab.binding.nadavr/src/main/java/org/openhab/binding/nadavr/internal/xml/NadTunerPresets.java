/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.nadavr.internal.xml;

import static org.openhab.binding.nadavr.internal.NadAvrBindingConstants.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NadTunerPresets.java} class is used to retrieve user defined tuner preset details from an xml file named
 * in the configuration.
 *
 * @author Dave J Schoepel - Initial contribution
 */

@NonNullByDefault
public class NadTunerPresets {
    private final Logger logger = LoggerFactory.getLogger(NadTunerPresets.class);

    protected List<NadPreset> preset = new ArrayList<NadPreset>();

    public List<NadPreset> getPreset() {
        return preset;
    }

    protected boolean validFileFormat = false;

    public void setPreset(List<NadPreset> preset) {
        this.preset = preset;
    }

    public boolean presetFileIsValid(String fileName) {
        parsePresets(fileName);
        return validFileFormat;
    }

    public List<NadPreset> parsePresets(String fileName) {
        validFileFormat = true;
        List<NadPreset> presetList = new ArrayList<>();
        NadPreset preset = new NadPreset("", "", "", "");
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        try {
            XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(new FileInputStream(fileName));
            while (xmlEventReader.hasNext()) {
                XMLEvent xmlEvent = xmlEventReader.nextEvent();
                if (xmlEvent.isStartElement()) {
                    StartElement startElement = xmlEvent.asStartElement();
                    switch (startElement.getName().getLocalPart()) {
                        case "preset":
                            // Get the 'id' attribute from preset element
                            preset = new NadPreset("", "", "", "");
                            Attribute id = startElement.getAttributeByName(new QName("id"));
                            if (id != null) {
                                preset.setId(id.getValue());
                            }
                            break;
                        case "band":
                            xmlEvent = xmlEventReader.nextEvent();
                            if (xmlEvent.isCharacters()) {
                                preset.setBand(xmlEvent.asCharacters().getData());
                            } else {
                                preset.setBand(NOT_SET);
                            }
                            break;
                        case "frequency":
                            xmlEvent = xmlEventReader.nextEvent();
                            if (xmlEvent.isCharacters()) {
                                preset.setFrequency(xmlEvent.asCharacters().getData());
                            } else {
                                preset.setFrequency(NOT_SET);
                            }
                            break;
                        case "name":
                            xmlEvent = xmlEventReader.nextEvent();
                            if (xmlEvent.isCharacters()) {
                                preset.setName(xmlEvent.asCharacters().getData());
                            } else {
                                preset.setName(NOT_SET);
                            }
                            break;
                    }
                }
                // if preset end element is reached, add preset object to list
                if (xmlEvent.isEndElement()) {
                    EndElement endElement = xmlEvent.asEndElement();
                    if (NAD_PRESET.equalsIgnoreCase(endElement.getName().getLocalPart())) {
                        if (!NOT_SET.equals(preset.getBand()) && !NOT_SET.equals(preset.getFrequency())
                                && !NOT_SET.equals(preset.getName())) {
                            presetList.add(preset);
                            logger.debug("Added NadPreset {} detail: Band = \"{}\", Frequency = \"{}\", Name = \"{}\"",
                                    preset.getID(), preset.getBand(), preset.getFrequency(), preset.getName());
                        } else {
                            logger.warn(
                                    "Skipped preset {} details: Band = \"{}\", Frequency = \"{}\", Name = \"{}\" Check for missing details in definition file {}!",
                                    preset.getID(), preset.getBand(), preset.getFrequency(), preset.getName(),
                                    fileName);
                            validFileFormat = false;
                        }
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            logger.error("XML Preset_Names Detail File was not found!  Error: {}", ex.getMessage());
            validFileFormat = false;
        } catch (XMLStreamException ex) {
            logger.error("Parsing File: {} found an error!  Error: {}", fileName, ex.getMessage());
            validFileFormat = false;
        }
        return presetList;
    }
}
