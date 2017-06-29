package fr.lirmm.graphik.graal.chase_bench;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.AtomSet;
import fr.lirmm.graphik.graal.api.core.AtomSetException;
import fr.lirmm.graphik.graal.api.core.ConjunctiveQuery;
import fr.lirmm.graphik.graal.api.core.Literal;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.api.core.RuleSet;
import fr.lirmm.graphik.graal.api.core.Substitution;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.graal.api.core.Variable;
import fr.lirmm.graphik.graal.api.forward_chaining.Chase;
import fr.lirmm.graphik.graal.api.forward_chaining.ChaseException;
import fr.lirmm.graphik.graal.api.forward_chaining.RuleApplicationException;
import fr.lirmm.graphik.graal.api.homomorphism.HomomorphismException;
import fr.lirmm.graphik.graal.api.io.GraalWriter;
import fr.lirmm.graphik.graal.api.io.Parser;
import fr.lirmm.graphik.graal.chase_bench.io.ChaseBenchDataParser;
import fr.lirmm.graphik.graal.chase_bench.io.ChaseBenchQueryParser;
import fr.lirmm.graphik.graal.chase_bench.io.ChaseBenchRuleParser;
import fr.lirmm.graphik.graal.chase_bench.io.ChaseBenchWriter;
import fr.lirmm.graphik.graal.core.DefaultConjunctiveQuery;
import fr.lirmm.graphik.graal.core.atomset.graph.DefaultInMemoryGraphStore;
import fr.lirmm.graphik.graal.core.compilation.IDCompilation;
import fr.lirmm.graphik.graal.core.factory.DefaultConjunctiveQueryFactory;
import fr.lirmm.graphik.graal.core.grd.DefaultGraphOfRuleDependencies;
import fr.lirmm.graphik.graal.core.ruleset.LinkedListRuleSet;
import fr.lirmm.graphik.graal.core.unifier.checker.AtomErasingChecker;
import fr.lirmm.graphik.graal.core.unifier.checker.ProductivityChecker;
import fr.lirmm.graphik.graal.forward_chaining.BreadthFirstChase;
import fr.lirmm.graphik.graal.forward_chaining.ChaseWithGRD;
import fr.lirmm.graphik.graal.forward_chaining.SccChase;
import fr.lirmm.graphik.graal.homomorphism.StaticHomomorphism;
import fr.lirmm.graphik.graal.store.rdbms.adhoc.AdHocRdbmsStore;
import fr.lirmm.graphik.graal.store.rdbms.driver.PostgreSQLDriver;
import fr.lirmm.graphik.util.profiler.CPUTimeProfiler;
import fr.lirmm.graphik.util.profiler.Profiler;
import fr.lirmm.graphik.util.stream.CloseableIterator;
import fr.lirmm.graphik.util.stream.CloseableIteratorWithoutException;
import fr.lirmm.graphik.util.stream.IteratorAdapter;
import fr.lirmm.graphik.util.stream.Iterators;

/**
 * @author Clément Sipieter (INRIA) {@literal <clement@6pi.fr>}
 *
 */
public class ChaseBench {

	@Parameter(names = { "-c", "--chase" }, description = "GRD|SCC|BREADTH")
	String chase = "SCC";
	
	@Parameter(names = { "-m", "--mode" }, description = "MEM|SQL InMemory or PostgreSQL")
	String mode = "MEM";

	@Parameter(names = { "--db" }, description = "database name")
	String databaseName = "";

	@Parameter(names = { "--host" }, description = "database host")
	String databaseHost = "localhost";

	@Parameter(names = { "--user" }, description = "database user")
	String databaseUser = "root";

	@Parameter(names = { "--password" }, description = "database password")
	String databasePassword = "root";

	@Parameter(names = { "-h", "--help" }, help = true)
	boolean help;

	@Parameter(names = { "--data" }, description = "Data directory path", required = true)
	String inputDataFilePath;

	@Parameter(names = { "--st-tgds" }, description = "Source to Target TGDS file path", required = true)
	String inputStTgdsFilePath;

	@Parameter(names = { "--t-tgds" }, description = "Target TGDS file path")
	String inputTargetTgdsFilePath = "";

	@Parameter(names = { "--queries" }, description = "queries directory path")
	String inputQueryDirPath = "";
	
	@Parameter(names = { "--lin" }, description = "Enable specific chase for linear ontologies")
	Boolean linear = false;
	
	@Parameter(names = { "-u, --uniq" }, description = "ensure unicity on query answers", arity = 1)
	boolean uniq = true;
	
	@Parameter(names = {"-C", "--compilation"}, arity = 1)
	boolean compilation = true;
	
	@Parameter(names = {"-O", "--opti"}, arity = 1)
	boolean opti = false;
	
	@Parameter(names = {"-v", "--verbose"})
	boolean verbose = false;
	
	//@Parameter(names = {"-a"})
	boolean aa = false;

	static String outputFilePath = "./output.txt";

	public static void main(String args[])
	    throws ChaseException, AtomSetException, IOException, HomomorphismException, SQLException, RuleApplicationException {
		ChaseBench options = new ChaseBench();
		JCommander commander = new JCommander(options, args);

		if (options.help) {
			commander.usage();
			System.exit(0);
		}

		RuleSet stTgdsSet = new LinkedListRuleSet();
		RuleSet targetTgdsSet = new LinkedListRuleSet();

		Profiler prof = new CPUTimeProfiler();
		prof.setOutputStream(System.err);
		prof.start("total time");

		// Database connection //
		// the database must exist and should be empty before running this
		// program.

		AtomSet atomSetDest;
		{
			AtomSet atomSetSrc;
			if ("SQL".equals(options.mode)) {
				// atomSet = new DefaultRdbmsStore(new PostgreSQLDriver("localhost",
				// "papotti", "clement", "clement"));
				atomSetSrc = new AdHocRdbmsStore(new PostgreSQLDriver(options.databaseHost, options.databaseName,
				                                                     options.databaseUser, options.databasePassword));
				atomSetDest = new DefaultInMemoryGraphStore();
	
			} else {
				// Alternatively, you can use an in memory graph based AtomSet
				atomSetSrc = new DefaultInMemoryGraphStore();
				atomSetDest = new DefaultInMemoryGraphStore();
			}
	
			// Parsing data //
			prof.start("parsing data");
			Parser<Atom> dataParser = new ChaseBenchDataParser(new File(options.inputDataFilePath));
			prof.stop("parsing data");
	
			// Loading data //
			prof.start("loading data");
			atomSetSrc.addAll(dataParser);
			prof.stop("loading data");
	
			// Loading rules //
			prof.start("parsing/loading st-tgds");
			Parser<Rule> ruleParser = new ChaseBenchRuleParser(new File(options.inputStTgdsFilePath));
			stTgdsSet.addAll(new IteratorAdapter<Rule>(ruleParser));
			prof.stop("parsing/loading st-tgds");
	
			if(!options.inputTargetTgdsFilePath.isEmpty()) {
				prof.start("parsing/loading t-tgds");
				ruleParser = new ChaseBenchRuleParser(new File(options.inputTargetTgdsFilePath));
				targetTgdsSet.addAll(new IteratorAdapter<Rule>(ruleParser));
				prof.stop("parsing/loading t-tgds");
			}
	
			// Applying chase //
			// The SimpleChase is a quickly optimized chase that works with the
			// attached snapshot version
			// condition.
			prof.start("total chase time");
			prof.start("st-tgds chase");
			{
				for(Rule r : stTgdsSet) { 					
					ConjunctiveQuery query = DefaultConjunctiveQueryFactory.instance().create(r.getBody(),
				        new LinkedList<Term>(r.getFrontier()));
					CloseableIterator<Substitution> subIt = StaticHomomorphism.instance().execute(query, atomSetSrc);
					while (subIt.hasNext()) {
						Substitution substitution = subIt.next();
						// replace variables by fresh symbol 
						for (Variable t : r.getExistentials()) { 
							substitution.put(t, atomSetDest.getFreshSymbolGenerator().getFreshSymbol()); 
						}
						atomSetDest.addAll(substitution.createImageOf(r.getHead()));
					}
					
				}

			}
			prof.stop("st-tgds chase");
		}
		

		/////////////////////////////////////////
		//
		
		Chase chase;
		if(!targetTgdsSet.isEmpty()) {
			boolean isLinear = false;
			if(options.linear) {
				prof.start("Check linearity");
				isLinear = true;
				for (Rule r : targetTgdsSet) {
					if (!isLinear(r)) {
						isLinear = false;
						break;
					}
				}
				prof.stop("Check linearity");
				prof.trace("tgds set linearity: " + isLinear);
		
		
			}
			chase = null;
			prof.start("preproccessing chase");
			if (isLinear) {
				chase = new BreadthFirstChase(targetTgdsSet, atomSetDest);
			} else {
				if(options.opti) {
					IDCompilation comp = new IDCompilation();
					comp.compile(targetTgdsSet.iterator());
					Chase c = new BreadthFirstChase(comp.getSaturation(), atomSetDest);
					c.next();
					
					for(Rule r : targetTgdsSet) {
						c = new BreadthFirstChase(comp.getSaturation(), r.getHead());
						c.next();
						System.out.println(r);
					}
					
				} else if(options.compilation) {
					prof.start("Rule compilation computing time");
					IDCompilation comp = new IDCompilation();
					comp.compile(targetTgdsSet.iterator());
					for(Rule r : comp.getSaturation()) {
						targetTgdsSet.add(r);
					}
					prof.stop("Rule compilation computing time");
				}
				
				prof.start("Graph of Rule Dependencies computing time");
				DefaultGraphOfRuleDependencies grd = new DefaultGraphOfRuleDependencies(targetTgdsSet.iterator(), false, AtomErasingChecker.instance(), ProductivityChecker.instance());
				prof.stop("Graph of Rule Dependencies computing time");
				if(options.chase.equals("GRD")) {
					chase = new ChaseWithGRD(grd, atomSetDest);
				} else if(options.chase.equals("SCC")) {
					chase = new SccChase(grd, atomSetDest);
				} else if(options.chase.equals("BREADTH")) {
					chase = new BreadthFirstChase(targetTgdsSet, atomSetDest);
				}
			}
			prof.stop("preproccessing chase");

			prof.put("chase", chase.getClass());
			prof.start("t-tgds chase");
			profile(chase);
			System.out.println("Nb terms :");
			System.out.println((Iterators.count(atomSetDest.termsIterator())));
			prof.stop("t-tgds chase");
	
		}
		prof.stop("total chase time");

		prof.start("writing saturated data");
		// Write data //
		// Now, we can write our saturated data in a new file.
		GraalWriter writer = new ChaseBenchWriter(outputFilePath);
		writer.write(atomSetDest);
		writer.close();
		prof.stop("writing saturated data");


		if (!options.inputQueryDirPath.isEmpty()) {
			prof.start("load queries");
			List<ConjunctiveQuery> queries = new LinkedList<ConjunctiveQuery>();

			File dir = new File(options.inputQueryDirPath);
			File[] files = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".txt");
				}
			});

			for (File f : files) {
				Parser<ConjunctiveQuery> queryParser = new ChaseBenchQueryParser(f);
				while (queryParser.hasNext()) {
					ConjunctiveQuery q = queryParser.next();
					q = new DefaultConjunctiveQuery(f.getName(), q.getAtomSet(), q.getAnswerVariables());
					queries.add(q);
				}
			}
			prof.stop("load queries");

			int i = 0;

			prof.start("total querying time");
			for (ConjunctiveQuery q : queries) {
				GraalWriter w = new ChaseBenchWriter(new File(q.getLabel()));
				prof.start("query " + i);
				StaticHomomorphism h = StaticHomomorphism.instance();
				//h.setProfiler(prof);
				CloseableIterator<Substitution> execute = h.execute(q, atomSetDest);
				execute = Iterators.uniq(execute);
				int nbAns = 0;
				while (execute.hasNext()) {
					++nbAns;
					Substitution s = execute.next();
					for (Term t : q.getAnswerVariables()) {
						writeTerm(w, s.createImageOf(t));
						w.write(", ");
					}
					w.write("\n");
				}
				prof.stop("query " + i);
				if(options.verbose) {
					prof.put("query " + Integer.toString(i) + "-#ans: ", nbAns);
				}
				w.close();
				++i;
			}
			prof.stop("total querying time");

		}
		prof.stop("total time");
		//System.err.println(prof);
	}

	/**
	 * @param chase2
	 * @throws ChaseException 
	 */
	private static void profile(Chase chase) throws ChaseException {
		chase.execute();
		System.gc();
	}

	protected static void writeTerm(GraalWriter w, Term t) throws IOException {
		if (t.isVariable()) {
			w.write("?" + t.getIdentifier());
		} else if (t.isLiteral()) {
			writeLiteral(w, (Literal) t);
		} else { // Constant
			w.write(t.getIdentifier());
		}
	}

	protected static void writeLiteral(GraalWriter w, Literal l) throws IOException {
		w.write("\"");
		w.write(l.getValue().toString());
		w.write("\"");
	}

	protected static boolean isLinear(Rule r) {
		CloseableIteratorWithoutException<Atom> it = r.getBody().iterator();
		if (it.hasNext()) {
			it.next();
			return !it.hasNext();
		} else {
			return true;
		}

	}
	

}
