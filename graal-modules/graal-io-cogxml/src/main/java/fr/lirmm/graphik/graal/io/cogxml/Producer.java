package fr.lirmm.graphik.graal.io.cogxml;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import fr.lirmm.graphik.util.stream.ArrayBlockingStream;

class Producer implements Runnable {

	private InputStream inputStream;
	private ArrayBlockingStream<Object> buffer;

	Producer(InputStream inputStream, ArrayBlockingStream<Object> buffer) {
		this.inputStream = inputStream;
		this.buffer = buffer;
	}

	public void run() {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser;
		try {
			parser = factory.newSAXParser();
		    parser.parse(this.inputStream, new CogxmlHandler(this.buffer));
		} catch (ParserConfigurationException | SAXException | IOException e) {
			buffer.write(new CogxmlParseException(e));
		} finally {
			buffer.close();
		}
	}
}