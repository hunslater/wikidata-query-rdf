package org.wikidata.query.rdf.tool.rdf;

import static org.hamcrest.Matchers.allOf;
import static org.wikidata.query.rdf.tool.Matchers.binds;
import static org.wikidata.query.rdf.tool.StatementHelper.siteLink;
import static org.wikidata.query.rdf.tool.StatementHelper.statement;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.Test;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.IntegerLiteralImpl;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.wikidata.query.rdf.common.uri.Ontology;
import org.wikidata.query.rdf.common.uri.Provenance;
import org.wikidata.query.rdf.common.uri.RDF;
import org.wikidata.query.rdf.common.uri.RDFS;
import org.wikidata.query.rdf.common.uri.SchemaDotOrg;
import org.wikidata.query.rdf.tool.AbstractRdfRepositoryIntegrationTestBase;

import com.google.common.collect.ImmutableList;

/**
 * Tests RdfRepository against a live RDF repository.
 */
public class RdfRepositoryIntegrationTest extends AbstractRdfRepositoryIntegrationTestBase {
    @Test
    public void newSiteLink() throws QueryEvaluationException {
        rdfRepository.sync("Q23", siteLink("Q23", "http://en.wikipedia.org/wiki/George_Washington", "en"));
        TupleQueryResult r = rdfRepository.query("SELECT * WHERE {?s <http://schema.org/about> ?o}");
        assertTrue(r.hasNext());
        assertThat(r.next(), allOf(//
                binds("s", "http://en.wikipedia.org/wiki/George_Washington"),//
                binds("o", "Q23")));
        assertFalse(r.hasNext());
    }

    @Test
    public void moveSiteLink() throws QueryEvaluationException {
        newSiteLink();
        rdfRepository.sync("Q23", siteLink("Q23", "http://en.wikipedia.org/wiki/George_Washingmoved", "en"));
        TupleQueryResult r = rdfRepository.query("SELECT * WHERE {?s <http://schema.org/about> ?o}");
        assertTrue(r.hasNext());
        assertThat(r.next(), allOf(//
                binds("s", "http://en.wikipedia.org/wiki/George_Washingmoved"),//
                binds("o", "Q23")));
        assertFalse(r.hasNext());
    }

    @Test
    public void newLabel() throws QueryEvaluationException {
        rdfRepository.sync("Q23", ImmutableList.of(//
                statement("Q23", RDFS.LABEL, new LiteralImpl("George Washington", "en"))));
        TupleQueryResult r = rdfRepository.query("SELECT * WHERE {?s ?p ?o}");
        assertTrue(r.hasNext());
        assertThat(r.next(), allOf(//
                binds("s", "Q23"),//
                binds("p", RDFS.LABEL),//
                binds("o", new LiteralImpl("George Washington", "en"))));
        assertFalse(r.hasNext());
    }

    @Test
    public void changedLabel() throws QueryEvaluationException {
        newLabel();
        rdfRepository.sync("Q23", ImmutableList.of(//
                statement("Q23", RDFS.LABEL, new LiteralImpl("George Washingmoved", "en"))));
        TupleQueryResult r = rdfRepository.query("SELECT * WHERE {?s ?p ?o}");
        assertTrue(r.hasNext());
        assertThat(r.next(), allOf(//
                binds("s", "Q23"),//
                binds("p", RDFS.LABEL),//
                binds("o", new LiteralImpl("George Washingmoved", "en"))));
        assertFalse(r.hasNext());
    }

    @Test
    public void newLabelWithQuotes() throws QueryEvaluationException {
        rdfRepository.sync("Q23", ImmutableList.of(//
                statement("Q23", RDFS.LABEL, new LiteralImpl("George \"Cherry Tree\" Washington", "en"))));
        TupleQueryResult r = rdfRepository.query("SELECT * WHERE {?s ?p ?o}");
        assertTrue(r.hasNext());
        assertThat(r.next(), allOf(//
                binds("s", "Q23"),//
                binds("p", RDFS.LABEL),//
                binds("o", new LiteralImpl("George \"Cherry Tree\" Washington", "en"))));
        assertFalse(r.hasNext());
    }

    @Test
    public void statementWithBackslash() throws QueryEvaluationException {
        rdfRepository.sync("Q42", ImmutableList.of(//
                statement("Q42", "P396", new LiteralImpl("IT\\ICCU\\RAVV\\034417"))));
        TupleQueryResult r = rdfRepository.query("SELECT * WHERE {?s ?p ?o}");
        assertTrue(r.hasNext());
        assertThat(r.next(), allOf(//
                binds("s", "Q42"),//
                binds("p", "P396"),//
                binds("o", new LiteralImpl("IT\\ICCU\\RAVV\\034417"))));
        assertFalse(r.hasNext());
    }

    @Test
    public void statementToEntityDoesntRemoveEntity() throws QueryEvaluationException {
        Statement link = statement("Q23", "P26", "Q191789");
        Statement onGeorge = statement("Q23", "P20", "Q494413");
        Statement onMartha = statement("Q191789", "P20", "Q731635");
        rdfRepository.sync("Q23", ImmutableList.of(link, onGeorge));
        rdfRepository.sync("Q191789", ImmutableList.of(onMartha));
        assertTrue(rdfRepository.ask(entityUris.prefixes(new StringBuilder())
                .append("ASK {entity:Q23 entity:P20 entity:Q494413 }").toString()));
        assertTrue(rdfRepository.ask(entityUris.prefixes(new StringBuilder())
                .append("ASK {entity:Q23 entity:P26 entity:Q191789 }").toString()));
        assertTrue(rdfRepository.ask(entityUris.prefixes(new StringBuilder())
                .append("ASK {entity:Q191789 entity:P20 entity:Q731635 }").toString()));

        rdfRepository.sync("Q23", ImmutableList.of(onGeorge));
        assertTrue(rdfRepository.ask(entityUris.prefixes(new StringBuilder())
                .append("ASK {entity:Q23 entity:P20 entity:Q494413 }").toString()));
        assertFalse(rdfRepository.ask(entityUris.prefixes(new StringBuilder())
                .append("ASK {entity:Q23 entity:P26 entity:Q191789 }").toString()));
        assertTrue(rdfRepository.ask(entityUris.prefixes(new StringBuilder())
                .append("ASK {entity:Q191789 entity:P20 entity:Q731635 }").toString()));
    }

    @Test
    public void newLabelLanguage() throws QueryEvaluationException {
        newLabel();
        rdfRepository.sync("Q23", ImmutableList.of(//
                statement("Q23", RDFS.LABEL, new LiteralImpl("George Washington", "en")),//
                statement("Q23", RDFS.LABEL, new LiteralImpl("George Washington", "de"))));
        TupleQueryResult r = rdfRepository.query("SELECT * WHERE {?s ?p ?o} ORDER BY ?o");
        assertTrue(r.hasNext());
        assertThat(r.next(), allOf(//
                binds("s", "Q23"),//
                binds("p", RDFS.LABEL),//
                binds("o", new LiteralImpl("George Washington", "de"))));
        assertTrue(r.hasNext());
        assertThat(r.next(), allOf(//
                binds("s", "Q23"),//
                binds("p", RDFS.LABEL),//
                binds("o", new LiteralImpl("George Washington", "en"))));
        assertFalse(r.hasNext());
    }

    @Test
    public void basicExpandedStatement() throws QueryEvaluationException {
        List<Statement> george = expandedStatement("ce976010-412f-637b-c687-9fd2d52dc140", "Q23", "P509", "Q356405",
                Ontology.NORMAL_RANK);
        rdfRepository.sync("Q23", george);
        StringBuilder query = Ontology.prefix(entityUris.prefixes(new StringBuilder()));
        query.append("SELECT * WHERE { entity:Q23 entity:P509 [ v:P509 ?cause; ontology:rank ontology:NormalRank ] }");
        TupleQueryResult r = rdfRepository.query(query.toString());
        assertTrue(r.hasNext());
        assertThat(r.next(), binds("cause", "Q356405"));
        assertFalse(r.hasNext());
    }

    @Test
    public void changedExpandedStatementValue() throws QueryEvaluationException {
        basicExpandedStatement();
        List<Statement> george = expandedStatement("ce976010-412f-637b-c687-9fd2d52dc140", "Q23", "P509", "Q3736439",
                Ontology.NORMAL_RANK);
        // Poor George Washington's cause of death is now duck
        rdfRepository.sync("Q23", george);
        StringBuilder query = Ontology.prefix(entityUris.prefixes(new StringBuilder()));
        query.append("SELECT * WHERE { entity:Q23 entity:P509 [ v:P509 ?cause; ontology:rank ontology:NormalRank ] }");
        TupleQueryResult r = rdfRepository.query(query.toString());
        assertTrue(r.hasNext());
        assertThat(r.next(), binds("cause", "Q3736439"));
        assertFalse(r.hasNext());
    }

    @Test
    public void changedExpandedStatementRank() throws QueryEvaluationException {
        basicExpandedStatement();
        List<Statement> george = expandedStatement("ce976010-412f-637b-c687-9fd2d52dc140", "Q23", "P509", "Q356405",
                Ontology.DEPRECATED_RANK);
        rdfRepository.sync("Q23", george);
        StringBuilder query = Ontology.prefix(entityUris.prefixes(new StringBuilder()));
        query.append("SELECT * WHERE { entity:Q23 entity:P509 [ v:P509 ?cause; ontology:rank ontology:DeprecatedRank ] }");
        TupleQueryResult r = rdfRepository.query(query.toString());
        assertTrue(r.hasNext());
        assertThat(r.next(), binds("cause", "Q356405"));
        assertFalse(r.hasNext());
    }

    @Test
    public void referencesOnExpandedStatements() throws QueryEvaluationException {
        String referenceUri = entityUris.reference().namespace() + "e36b7373814a0b74caa84a5fc2b1e3297060ab0f";
        List<Statement> george = expandedStatement("9D3713FF-7BCC-489F-9386-C7322C0AC284", "Q23", "P19", "Q494413",
                Ontology.NORMAL_RANK, referenceUri);
        statement(george, referenceUri, entityUris.value().namespace() + "P854",
                "http://www.anb.org/articles/02/02-00332.html");
        rdfRepository.sync("Q23", george);
        StringBuilder query = Provenance.prefix(Ontology.prefix(entityUris.prefixes(new StringBuilder())));
        query.append("SELECT * WHERE { entity:Q23 entity:P19 [ v:P19 ?placeOfBirth; prov:wasDerivedFrom [ ?provP ?provO ] ] }");
        TupleQueryResult r = rdfRepository.query(query.toString());
        assertTrue(r.hasNext());
        assertThat(r.next(), allOf(//
                binds("placeOfBirth", "Q494413"),//
                binds("provP", entityUris.value().namespace() + "P854"),//
                binds("provO", "http://www.anb.org/articles/02/02-00332.html")));
        assertFalse(r.hasNext());
    }

    @Test
    public void referencesOnExpandedStatementsChangeValue() throws QueryEvaluationException {
        referencesOnExpandedStatements();
        String referenceUri = entityUris.reference().namespace() + "e36b7373814a0b74caa84a5fc2b1e3297060ab0f";
        List<Statement> george = expandedStatement("9D3713FF-7BCC-489F-9386-C7322C0AC284", "Q23", "P19", "Q494413",
                Ontology.NORMAL_RANK, referenceUri);
        statement(george, referenceUri, entityUris.value().namespace() + "P854", "http://example.com");
        rdfRepository.sync("Q23", george);
        StringBuilder query = Provenance.prefix(Ontology.prefix(entityUris.prefixes(new StringBuilder())));
        query.append("SELECT * WHERE { entity:Q23 entity:P19 [ v:P19 ?placeOfBirth; prov:wasDerivedFrom [ ?provP ?provO ] ] }");
        TupleQueryResult r = rdfRepository.query(query.toString());
        assertTrue(r.hasNext());
        assertThat(r.next(), allOf(//
                binds("placeOfBirth", "Q494413"),//
                binds("provP", entityUris.value().namespace() + "P854"),//
                binds("provO", "http://example.com")));
        assertFalse(r.hasNext());
    }

    @Test
    public void referencesOnExpandedStatementsChangePredicate() throws QueryEvaluationException {
        referencesOnExpandedStatements();
        String referenceUri = entityUris.reference().namespace() + "e36b7373814a0b74caa84a5fc2b1e3297060ab0f";
        List<Statement> george = expandedStatement("9D3713FF-7BCC-489F-9386-C7322C0AC284", "Q23", "P19", "Q494413",
                Ontology.NORMAL_RANK, referenceUri);
        statement(george, referenceUri, entityUris.value().namespace() + "P143",
                "http://www.anb.org/articles/02/02-00332.html");
        rdfRepository.sync("Q23", george);
        StringBuilder query = Provenance.prefix(Ontology.prefix(entityUris.prefixes(new StringBuilder())));
        query.append("SELECT * WHERE { entity:Q23 entity:P19 [ v:P19 ?placeOfBirth; prov:wasDerivedFrom [ ?provP ?provO ] ] }");
        TupleQueryResult r = rdfRepository.query(query.toString());
        assertTrue(r.hasNext());
        assertThat(r.next(), allOf(//
                binds("placeOfBirth", "Q494413"),//
                binds("provP", entityUris.value().namespace() + "P143"),//
                binds("provO", "http://www.anb.org/articles/02/02-00332.html")));
        assertFalse(r.hasNext());
    }

    @Test
    public void sharedReference() throws QueryEvaluationException {
        // Load a shared reference
        String referenceUri = entityUris.reference().namespace() + "e36b7373814a0b74caa84a5fc2b1e3297060ab0f";
        List<Statement> george = expandedStatement("9D3713FF-7BCC-489F-9386-C7322C0AC284", "Q23", "P19", "Q494413",
                Ontology.NORMAL_RANK, referenceUri);
        Statement refDecl = statement(george, referenceUri, entityUris.value().namespace() + "P854",
                "http://www.anb.org/articles/02/02-00332.html");
        rdfRepository.sync("Q23", george);
        List<Statement> dummy = expandedStatement("9D3713FF-7BCC-489F-9386-C7322C0AC284", "Q1234134", "P19", "Q494413",
                Ontology.NORMAL_RANK, referenceUri);
        dummy.add(refDecl);
        rdfRepository.sync("Q1234134", dummy);

        // Now query and make sure you can find it
        StringBuilder query = Provenance.prefix(Ontology.prefix(entityUris.prefixes(new StringBuilder())));
        query.append("SELECT * WHERE { entity:Q23 entity:P19 [ v:P19 ?placeOfBirth; prov:wasDerivedFrom [ ?provP ?provO ] ] }");
        TupleQueryResult r = rdfRepository.query(query.toString());
        assertTrue(r.hasNext());
        assertThat(r.next(), allOf(//
                binds("placeOfBirth", "Q494413"),//
                binds("provP", entityUris.value().namespace() + "P854"),//
                binds("provO", "http://www.anb.org/articles/02/02-00332.html")));
        assertFalse(r.hasNext());

        // Now remove the reference from just one place
        dummy = expandedStatement("9D3713FF-7BCC-489F-9386-C7322C0AC284", "Q1234134", "P19", "Q494413",
                Ontology.NORMAL_RANK);
        rdfRepository.sync("Q1234134", dummy);

        // Now query and find the reference still there because its shared!
        query = Provenance.prefix(Ontology.prefix(entityUris.prefixes(new StringBuilder())));
        query.append("SELECT * WHERE { entity:Q23 entity:P19 [ v:P19 ?placeOfBirth; prov:wasDerivedFrom [ ?provP ?provO ] ] }");
        r = rdfRepository.query(query.toString());
        assertTrue(r.hasNext());
        assertThat(r.next(), allOf(//
                binds("placeOfBirth", "Q494413"),//
                binds("provP", entityUris.value().namespace() + "P854"),//
                binds("provO", "http://www.anb.org/articles/02/02-00332.html")));
        assertFalse(r.hasNext());

        // Query one more way just to be sure it works 10000%
        query = Ontology.prefix(entityUris.prefixes(new StringBuilder()));
        query.append("SELECT * WHERE { ?s ?p ?o . FILTER( STRSTARTS(STR(?s), \"")
                .append(entityUris.reference().namespace()).append("\") ) . }");
        r = rdfRepository.query(query.toString());
        assertTrue(r.hasNext());
        assertThat(r.next(), allOf(//
                binds("p", entityUris.value().namespace() + "P854"),//
                binds("o", "http://www.anb.org/articles/02/02-00332.html")));
        assertFalse(r.hasNext());

        // Now remove it from its last place
        george = expandedStatement("9D3713FF-7BCC-489F-9386-C7322C0AC284", "Q23", "P19", "Q494413",
                Ontology.NORMAL_RANK);
        rdfRepository.sync("Q23", george);

        /*
         * Now query and find the reference now gone because it isn't used
         * anywhere.
         */
        query = Ontology.prefix(entityUris.prefixes(new StringBuilder()));
        query.append("SELECT * WHERE { ?s ?p ?o . FILTER( STRSTARTS(STR(?s), \"")
                .append(entityUris.reference().namespace()).append("\") ) . }");
        r = rdfRepository.query(query.toString());
        assertFalse(r.hasNext());
    }

    private List<Statement> expandedStatement(String statementId, String subject, String predicate, String value,
            String rank, String referenceUri) {
        List<Statement> statements = expandedStatement(statementId, subject, predicate, value, rank);
        String statementUri = statements.get(1).getSubject().stringValue();
        statement(statements, statementUri, Provenance.WAS_DERIVED_FROM, referenceUri);
        return statements;
    }

    private List<Statement> expandedStatement(String statementId, String subject, String predicate, String value,
            String rank) {
        List<Statement> statements = new ArrayList<>();
        String statementUri = entityUris.statement().namespace() + subject + "-" + statementId;
        statement(statements, subject, predicate, statementUri);
        statement(statements, statementUri, entityUris.value().namespace() + predicate, value);
        statement(statements, statementUri, Ontology.RANK, rank);
        return statements;
    }

    @Test
    public void hasRevisionFalseIfNotPresent() {
        assertFalse(rdfRepository.hasRevision("Q23", 10));
    }

    @Test
    public void hasRevisionFalseIfTooEarly() {
        syncJustVersion("Q23", 1);
        assertFalse(rdfRepository.hasRevision("Q23", 10));
    }

    @Test
    public void hasRevisionTrueIfMatch() {
        syncJustVersion("Q23", 10);
        assertTrue(rdfRepository.hasRevision("Q23", 10));
    }

    @Test
    public void hasRevisionTrueIfAfter() {
        syncJustVersion("Q23", 10);
        assertTrue(rdfRepository.hasRevision("Q23", 9));
    }

    /**
     * Updating items with lots of sitelinks shouldn't be painfully slow.
     */
    @Test
    public void repeatedSiteLinksArentModified() throws QueryEvaluationException {
        /*
         * Note that this used to verify that noop updates for site links was
         * fast and now it just verifies that they don't cause any reported
         * modifications. Its not as strong an assertion but its less dependent
         * on external conditions.
         */
        List<Statement> statements = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String link = String.format(Locale.ROOT, "http://%s.example.com/wiki/tbl", i);
            statements.add(statement(link, RDF.TYPE, SchemaDotOrg.ARTICLE));
            statements.add(statement(link, SchemaDotOrg.IN_LANGUAGE, new LiteralImpl(Integer.toString(i))));
            statements.add(statement(link, SchemaDotOrg.ABOUT, "Q80"));
        }
        rdfRepository.sync("Q80", statements);
        assertEquals(0, rdfRepository.sync("Q80", statements));
        TupleQueryResult r = rdfRepository
                .query("PREFIX entity: <http://www.wikidata.org/entity/>\nSELECT (COUNT(?s) as ?sc) WHERE {?s ?p entity:Q80}");
        assertTrue(r.hasNext());
        assertThat(r.next(), binds("sc", new IntegerLiteralImpl(BigInteger.valueOf(10))));
        assertFalse(r.hasNext());
    }

    /**
     * Updating items with lots of statements shouldn't be painfully slow.
     */
    @Test
    public void repeatedStatementsArentModified() throws QueryEvaluationException {
        /*
         * Note that this used to verify that noop updates for statements was
         * fast and now it just verifies that they don't cause any reported
         * modifications. Its not as strong an assertion but its less dependent
         * on external conditions.
         */
        List<Statement> statements = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            statements.add(statement("Q80", "P" + i, new IntegerLiteralImpl(BigInteger.valueOf(i))));
        }
        rdfRepository.sync("Q80", statements);
        assertEquals(0, rdfRepository.sync("Q80", statements));
        TupleQueryResult r = rdfRepository
                .query("PREFIX entity: <http://www.wikidata.org/entity/>\nSELECT (COUNT(?p) as ?sc) WHERE {entity:Q80 ?p ?o}");
        assertTrue(r.hasNext());
        assertThat(r.next(), binds("sc", new IntegerLiteralImpl(BigInteger.valueOf(10))));
        assertFalse(r.hasNext());
    }

    @Test
    public void delete() throws QueryEvaluationException {
        newSiteLink();
        rdfRepository.sync("Q23", Collections.<Statement> emptyList());
        TupleQueryResult r = rdfRepository.query("SELECT * WHERE {?s ?p ?o}");
        assertFalse(r.hasNext());
    }

    private void syncJustVersion(String entityId, int version) {
        Statement statement = statement(entityId, SchemaDotOrg.VERSION,
                new IntegerLiteralImpl(new BigInteger(Integer.toString(version))));
        rdfRepository.sync(entityId, ImmutableList.of(statement));
    }
}