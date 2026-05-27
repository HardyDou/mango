package cn.keking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerMainTests {
	@Test
	void contextLoads() {
		assert ServerMain.class != null;
	}

	@Test
	void standaloneServerProperties_mapEnginePortToSpringBootServerPort() {
		assertEquals("${mango.file-preview.engine.port:8012}", ServerMain.standaloneServerProperties().get("server.port"));
	}
}
