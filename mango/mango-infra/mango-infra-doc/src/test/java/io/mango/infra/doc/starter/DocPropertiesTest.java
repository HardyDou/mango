package io.mango.infra.doc.starter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocPropertiesTest {

    @Test
    void pathsToMatchShouldNotExposeMutableArray() {
        DocProperties properties = new DocProperties();
        String[] configuredPaths = {"/configured/**"};

        properties.setPathsToMatch(configuredPaths);
        configuredPaths[0] = "/mutated-source/**";
        assertEquals("/configured/**", properties.getPathsToMatch()[0]);

        String[] returnedPaths = properties.getPathsToMatch();
        returnedPaths[0] = "/mutated-result/**";
        assertEquals("/configured/**", properties.getPathsToMatch()[0]);
    }

    @Test
    void nestedConfigurationShouldNotExposeMutableObjects() {
        DocProperties properties = new DocProperties();
        DocProperties.Contact contact = new DocProperties.Contact();
        contact.setName("Configured Team");
        properties.setContact(contact);

        contact.setName("Mutated Source");
        assertEquals("Configured Team", properties.getContact().getName());

        DocProperties.Contact returnedContact = properties.getContact();
        returnedContact.setName("Mutated Result");
        assertEquals("Configured Team", properties.getContact().getName());
    }
}
