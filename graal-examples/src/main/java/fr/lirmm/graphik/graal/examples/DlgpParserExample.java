/*
 * Copyright (C) Inria Sophia Antipolis - Méditerranée / LIRMM
 * (Université de Montpellier & CNRS) (2014 - 2016)
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
package fr.lirmm.graphik.graal.examples;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.ConjunctiveQuery;
import fr.lirmm.graphik.graal.api.core.InMemoryAtomSet;
import fr.lirmm.graphik.graal.api.core.NegativeConstraint;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.api.io.GraalWriter;
import fr.lirmm.graphik.graal.core.atomset.LinkedListAtomSet;
import fr.lirmm.graphik.graal.io.dlp.DlgpParser;
import fr.lirmm.graphik.graal.io.dlp.DlgpWriter;

/**
 * @author Clément Sipieter (INRIA) <clement@6pi.fr>
 *
 */
public class DlgpParserExample {

	public static class Handler implements Thread.UncaughtExceptionHandler {

		private GraalWriter writer;

		/**
		 * @param writer
		 */
		public Handler(GraalWriter writer) {
			this.writer = writer;
		}

		@Override
		public void uncaughtException(Thread arg0, Throwable arg1) {
			try {
				writer.write("An error occured");
			} catch (IOException e) {
			}
		}

	}

	public static void main(String[] args) throws IOException {
		DlgpWriter writer = new DlgpWriter();
		writer.write("%% read exemple1.dlp\n");
		DlgpParser parser = new DlgpParser(new BufferedReader(new FileReader("./src/main/resources/example.dlp")),
		                                   new Handler(writer));

		while (parser.hasNext()) {
			Object o = parser.next();
			if (o instanceof NegativeConstraint)
				writer.write((NegativeConstraint) o);
			else if (o instanceof Rule)
				writer.write((Rule) o);
			else if (o instanceof Atom)
				writer.write((Atom) o);
			else if (o instanceof ConjunctiveQuery)
				writer.write((ConjunctiveQuery) o);
		}
		parser.close();

		writer.write("\n%% read exemple2.dlp\n");

		parser = new DlgpParser(new BufferedReader(new FileReader("./src/main/resources/example2.dlp")),
		                        new Handler(writer));

		while (parser.hasNext()) {
			Object o = parser.next();
			if (o instanceof NegativeConstraint)
				writer.write((NegativeConstraint) o);
			else if (o instanceof Rule)
				writer.write((Rule) o);
			else if (o instanceof Atom)
				writer.write((Atom) o);
			else if (o instanceof ConjunctiveQuery)
				writer.write((ConjunctiveQuery) o);
		}
		parser.close();

		writer.write("\n%% read dlp from java\n");

		try {
			writer.write(DlgpParser.parseAtom("p(a,b)."));
		} catch (Exception e) {
			writer.write("An error occured");
		}

		try {
			writer.write(DlgpParser.parseRule("p(X,Y) :- q(Y,X)."));
		} catch (Exception e) {
			writer.write("An error occured");
		}

		try {
			writer.write(DlgpParser.parseQuery(" ?(X) :- p(X,Y), q(Y,X)."));
		} catch (Exception e) {
			writer.write("An error occured");
		}

		try {
			writer.write((NegativeConstraint) DlgpParser.parseNegativeConstraint("! :- p(X,Y), q(X,Y)."));
		} catch (Exception e) {
			writer.write("An error occured");
		}

		try {
			InMemoryAtomSet atomset = new LinkedListAtomSet(DlgpParser.parseAtomSet("p(a,b), q(b,c)."));
			writer.write(atomset);
		} catch (Exception e) {
			writer.write("An error occured");
		}

		writer.close();

	}
};
