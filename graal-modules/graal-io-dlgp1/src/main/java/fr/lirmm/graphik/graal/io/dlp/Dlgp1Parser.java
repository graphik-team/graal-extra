/*
 * Copyright (C) Inria Sophia Antipolis - Méditerranée / LIRMM
 * (Université de Montpellier & CNRS) (2014 - 2015)
 *
 * Contributors :
 *
 * Clément SIPIETER <clement.sipieter@inria.fr>
 * Mélanie KÖNIG
 * Swan ROCHER
 * Jean-François BAGET
 * Michel LECLÈRE
 * Marie-Laure MUGNIER <mugnier@lirmm.fr>
 *
 *
 * This file is part of Graal <https://graphik-team.github.io/graal/>.
 *
 * This software is governed by the CeCILL  license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */
 /**
 * 
 */
package fr.lirmm.graphik.graal.io.dlp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.AtomSetException;
import fr.lirmm.graphik.graal.api.core.ConjunctiveQuery;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.graal.api.io.ParseError;
import fr.lirmm.graphik.graal.api.io.Parser;
import fr.lirmm.graphik.graal.api.kb.KnowledgeBase;
import fr.lirmm.graphik.graal.core.DefaultAtom;
import fr.lirmm.graphik.graal.core.DefaultNegativeConstraint;
import fr.lirmm.graphik.graal.core.stream.filter.AtomFilterIterator;
import fr.lirmm.graphik.graal.core.term.DefaultTermFactory;
import fr.lirmm.graphik.util.stream.AbstractCloseableIterator;
import fr.lirmm.graphik.util.stream.ArrayBlockingStream;
import fr.lirmm.graphik.util.stream.CloseableIterator;
import parser.DatalogGrammar;
import parser.ParseException;
import parser.TERM_TYPE;
import parser.TermFactory;

/**
 * 
 * @author Clément Sipieter (INRIA) <clement@6pi.fr>
 * 
 */
public final class Dlgp1Parser extends AbstractCloseableIterator<Object> implements Parser<Object>  {
	
	private static final Logger LOGGER = LoggerFactory
			.getLogger(Dlgp1Parser.class);

	private ArrayBlockingStream<Object> buffer = new ArrayBlockingStream<Object>(
			512);

	private static class DlgpListener extends AbstractDlgp1Listener {

		private ArrayBlockingStream<Object> set;

		DlgpListener(ArrayBlockingStream<Object> buffer) {
			this.set = buffer;
		}

		@Override
		protected void createAtom(DefaultAtom atom) {
			this.set.write(atom);
		}

		@Override
		protected void createQuery(ConjunctiveQuery query) {
			this.set.write(query);
		}

		@Override
		protected void createRule(Rule rule) {
			this.set.write(rule);
		}

		@Override
		protected void createNegConstraint(DefaultNegativeConstraint negativeConstraint) {
			this.set.write(negativeConstraint);
		}
	};

	private static class InternalTermFactory implements TermFactory {

		@Override
		public Term createTerm(TERM_TYPE termType, Object term) {
			switch(termType) {
			case ANSWER_VARIABLE:
			case VARIABLE:
				return DefaultTermFactory.instance().createVariable(
						(String) term);
			case CONSTANT: 
				return DefaultTermFactory.instance().createConstant(
						(String) term);
			case FLOAT:
			case INTEGER:
			case STRING:
				return DefaultTermFactory.instance().createLiteral(term);
			}
			return null;
		}
	}

	private static class Producer implements Runnable {

		private Reader reader;
		private ArrayBlockingStream<Object> buffer;

		Producer(Reader reader, ArrayBlockingStream<Object> buffer) {
			this.reader = reader;
			this.buffer = buffer;
		}

		@Override
		public void run() {
			DatalogGrammar dlpGrammar = new DatalogGrammar(
					new InternalTermFactory(), reader);
			dlpGrammar.addParserListener(new DlgpListener(buffer));
			try {
				dlpGrammar.document();
			} catch (ParseException e) {
				throw new ParseError("An error occured while parsing", e);
			} finally {
				buffer.close();
			}
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// CONSTRUCTOR
	// /////////////////////////////////////////////////////////////////////////
	
	private Reader reader = null;

	/**
	 * Constructor for parsing from the given reader.
	 * @param reader
	 */
	public Dlgp1Parser(Reader reader) {
		this.reader = reader;
		new Thread(new Producer(reader,buffer)).start();
	}
	
	/**
	 * Constructor for parsing from the standard input.
	 */
	public Dlgp1Parser() {
		this(new InputStreamReader(System.in));
	}
	
	/**
	 * Constructor for parsing from the given file.
	 * @param file
	 * @throws FileNotFoundException
	 */
	public Dlgp1Parser(File file) throws FileNotFoundException {
		this(new FileReader(file));
	}

	/**
	 * Constructor for parsing the content of the string s as DLGP content.
	 * @param s
	 */
	public Dlgp1Parser(String s) {
		this(new StringReader(s));
	}
	
	/**
	 * Constructor for parsing the given InputStream.
	 * @param in
	 */
	public Dlgp1Parser(InputStream in) {
		this(new InputStreamReader(in));
	}
	
	@Override
	protected void finalize() throws Throwable {
		this.close();
		super.finalize();
	}

	// /////////////////////////////////////////////////////////////////////////
	// METHODS
	// /////////////////////////////////////////////////////////////////////////

	@Override
	public boolean hasNext() {
		return buffer.hasNext();
	}

	@Override
	public Object next() {
		return buffer.next();
	}
	
	/**
	 * Closes the stream and releases any system resources associated with it.
	 * Closing a previously closed parser has no effect.
	 * 
	 * @throws IOException
	 */
	@Override
	public void close() {
		if(this.reader != null) {
			try {
				this.reader.close();
			} catch (IOException e) {
				LOGGER.error("Error during closing reader", e);
			}
			this.reader = null;
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// STATIC METHODS
	// /////////////////////////////////////////////////////////////////////////

	public static ConjunctiveQuery parseQuery(String s) {
		return (ConjunctiveQuery) new Dlgp1Parser(s).next();
	}

	public static Atom parseAtom(String s) {
		return (Atom) new Dlgp1Parser(s).next();
	}
	
	public static CloseableIterator<Atom> parseAtomSet(String s) {
		return new AtomFilterIterator(new Dlgp1Parser(s));
	}
	
	public static Rule parseRule(String s) {
		return (Rule) new Dlgp1Parser(s).next();
	}
	
	public static DefaultNegativeConstraint parseNegativeConstraint(String s) {
		return (DefaultNegativeConstraint) new Dlgp1Parser(s).next();
	}
	
	/**
	 * Parse a DLP content and store data into the KnowledgeBase target.
	 * 
	 * @param src
	 * @param target
	 * @throws AtomSetException 
	 */
	public static void parseKnowledgeBase(Reader src, KnowledgeBase target) throws AtomSetException {
		Dlgp1Parser parser = new Dlgp1Parser(src);

		while(parser.hasNext()) {
			Object o = parser.next();
			if (o instanceof Rule) {
				target.getOntology().add((Rule) o);
			} else if (o instanceof Atom) {
				target.getFacts().add((Atom) o);
			}
		}
	}

};
