package fr.lirmm.graphik.graal.io.cogxml;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.xml.sax.helpers.DefaultHandler;

import fr.lirmm.graphik.graal.api.core.Literal;
import fr.lirmm.graphik.graal.api.core.Predicate;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.graal.core.factory.DefaultAtomFactory;
import fr.lirmm.graphik.graal.core.factory.DefaultPredicateFactory;
import fr.lirmm.graphik.graal.core.term.DefaultTermFactory;
import fr.lirmm.graphik.util.DefaultURI;
import fr.lirmm.graphik.util.URIUtils;
import fr.lirmm.graphik.util.stream.InMemoryStream;


class CogxmlHandler extends DefaultHandler {
	
	private enum State {
		DEFAULT,
		CTYPE,
		RTYPE
	}

	private InMemoryStream<Object> set;
	private State state = State.DEFAULT;
	private Map<String, Integer> predicateArityMap;
	private Map<String, String> relationMapPredicate;
	private Map<String, List<Term>> relationMapTerms;
	private Map<String, Term> conceptMap;

	
	private static final Predicate PRED_LABEL = DefaultPredicateFactory.instance().create("label", 2);
	
	

	CogxmlHandler(InMemoryStream<Object> buffer) {
		this.set = buffer;
		predicateArityMap = new HashMap<>();
	}

	public void startDocument() {

	}

	public void startElement(String namespaceURI, String lname, String qname, Attributes attrs) throws SAXException {
		String rid = null;
		String cid = null;
		Term id = null;
		Literal label = null;
		Predicate type = null;
		int idx = 0;
		int arity = 0;
		
		switch(qname) {
			case "ctype" :
				id = DefaultTermFactory.instance().createConstant(URIUtils.createURI(attrs.getValue("id")));
				label = DefaultTermFactory.instance().createLiteral(URIUtils.RDF_LANG_STRING, attrs.getValue("label"));
				this.set.write(DefaultAtomFactory.instance().create(PRED_LABEL, id, label));
			break;
			case "rtype" :
				id = DefaultTermFactory.instance().createConstant(URIUtils.createURI(attrs.getValue("id")));
				label = DefaultTermFactory.instance().createLiteral(URIUtils.RDF_LANG_STRING, attrs.getValue("label"));
				arity = attrs.getValue("idSignature").split(" ").length;
				this.predicateArityMap.put(attrs.getValue("id"), arity);
				this.set.write(DefaultAtomFactory.instance().create(PRED_LABEL, id, label));
			break;
			case "marker" :
				id = DefaultTermFactory.instance().createConstant(new DefaultURI(attrs.getValue("id")));
				type = DefaultPredicateFactory.instance().create(URIUtils.createURI(attrs.getValue("idType")), 1);
				label = DefaultTermFactory.instance().createLiteral(URIUtils.RDF_LANG_STRING, attrs.getValue("label"));
				this.set.write(DefaultAtomFactory.instance().create(type, id));
				this.set.write(DefaultAtomFactory.instance().create(PRED_LABEL, id, label));
			break;
			case "concept" :
				String idMarker = attrs.getValue("idMarker");
				if(idMarker == null) {
					// FIXME datatype
				} else {
    				id = DefaultTermFactory.instance().createConstant(new DefaultURI(idMarker));
    				type = DefaultPredicateFactory.instance().create(URIUtils.createURI(attrs.getValue("idType")), 1);
    				this.conceptMap.put(attrs.getValue("id"), id);
    				this.set.write(DefaultAtomFactory.instance().create(type, id));
				}
				break;
			case "graph" :
				this.relationMapPredicate = new HashMap<>();
				this.relationMapTerms = new HashMap<>();
				this.conceptMap = new HashMap<>();
				break;
			case "relation" :
				String relId = attrs.getValue("idType");
				List terms = new ArrayList<>();
				for(int i = 0; i<this.predicateArityMap.get(relId); ++i) {
					terms.add(null);
				}
				this.relationMapPredicate.put(attrs.getValue("id"), relId);
				this.relationMapTerms.put(attrs.getValue("id"), terms);
				break;
			case "edge" :
				rid = attrs.getValue("rid");
				cid = attrs.getValue("cid");
				idx = Integer.valueOf(attrs.getValue("label")) - 1;
				this.relationMapTerms.get(rid).set(idx, this.conceptMap.get(cid)); // FIXME can not already exist
				break;
		}

	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		switch(qName) {
			case "graph" :
				for(Map.Entry<String, List<Term>> e : this.relationMapTerms.entrySet()) {
					this.set.write(DefaultAtomFactory.instance().create(
						DefaultPredicateFactory.instance().create(
							URIUtils.createURI(this.relationMapPredicate.get(e.getKey())), 
							e.getValue().size()), e.getValue()));
				}
				this.relationMapPredicate.clear();
				this.relationMapTerms.clear();
				this.conceptMap.clear();
				break;
		}
	}

	public void endDocument() {

	}

}