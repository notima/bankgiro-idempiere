package org.notima.idempiere.iso20022;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;

import org.junit.BeforeClass;
import org.junit.Test;

import iso.std.iso._20022.tech.xsd.camt_054_001.AccountNotification2;
import iso.std.iso._20022.tech.xsd.camt_054_001.CreditDebitCode;
import iso.std.iso._20022.tech.xsd.camt_054_001.DocumentCAMT54;
import iso.std.iso._20022.tech.xsd.camt_054_001.EntryDetails1;
import iso.std.iso._20022.tech.xsd.camt_054_001.EntryTransaction2;
import iso.std.iso._20022.tech.xsd.camt_054_001.RemittanceInformation5;
import iso.std.iso._20022.tech.xsd.camt_054_001.ReportEntry2;
import iso.std.iso._20022.tech.xsd.camt_054_001.TransactionReferences2;

/**
 * Tests reading of CAMT.054 files and resolving of invoice references.
 *
 * The files in src/test/resources/camt named *.TEST_* are anonymized real files
 * from Handelsbanken (format as of 2026). Names, account numbers, org numbers
 * and references are fake, amounts and structure are kept.
 */
public class CAMT54ReferenceResolverTest {

	private static JAXBContext context;

	/** One resolved transaction, flattened for easy comparison. */
	static class Tx {
		CreditDebitCode code;
		BigDecimal amount;
		CAMT54ReferenceResolver.Result result;
	}

	@BeforeClass
	public static void init() throws Exception {
		context = JAXBContext.newInstance(DocumentCAMT54.class);
	}

	private static DocumentCAMT54 read(String name) throws Exception {
		InputStream is = CAMT54ReferenceResolverTest.class.getResourceAsStream("/camt/" + name);
		assertNotNull("Test file missing: " + name, is);
		try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
			return (DocumentCAMT54) context.createUnmarshaller().unmarshal(reader);
		}
	}

	private static List<Tx> resolveAll(String name) throws Exception {
		DocumentCAMT54 doc = read(name);
		List<Tx> result = new ArrayList<Tx>();
		for (AccountNotification2 n : doc.getBkToCstmrDbtCdtNtfctn().getNtfctn()) {
			for (ReportEntry2 e : n.getNtry()) {
				assertNotNull("Booking date missing", e.getBookgDt().getDt());
				BigDecimal entrySum = BigDecimal.ZERO;
				for (EntryDetails1 det : e.getNtryDtls()) {
					for (EntryTransaction2 t : det.getTxDtls()) {
						Tx tx = new Tx();
						tx.code = e.getCdtDbtInd();
						tx.amount = t.getAmtDtls().getTxAmt().getAmt().getValue();
						tx.result = CAMT54ReferenceResolver.resolve(t, e.getCdtDbtInd());
						entrySum = entrySum.add(tx.amount);
						result.add(tx);
					}
				}
				// Batched entries: the transactions must add up to the entry amount
				assertEquals("Sum of transactions in entry " + e.getNtryRef(), 0, e.getAmt().getValue().compareTo(entrySum));
			}
		}
		return result;
	}

	private static void assertTx(Tx tx, CreditDebitCode code, String amount, String invoiceNo, String ocr, String name) {
		assertEquals(code, tx.code);
		assertEquals(0, new BigDecimal(amount).compareTo(tx.amount));
		assertEquals("invoiceNo for " + amount, invoiceNo, tx.result.invoiceNo);
		assertEquals("OCR for " + amount, ocr, tx.result.paymentReference);
		assertEquals("Name for " + amount, name, tx.result.counterpartyName);
	}

	/**
	 * Outgoing payments: EndToEndId is our invoice no. Name is the creditor's,
	 * not our own (which Handelsbanken now includes as Dbtr).
	 */
	@Test
	public void outgoingPayments2026() throws Exception {
		List<Tx> l = resolveAll("GEUT_CAMTDEB_20261102_215939.TEST_utbet.xml");
		assertEquals(6, l.size());
		// Creditor has OCR (SCOR), but EndToEndId (ours) takes precedence
		assertTx(l.get(0), CreditDebitCode.DBIT, "434.00", "4658087", null, "Motpart 1 AB");
		// Creditor has invoice number (RfrdDocInf)
		assertTx(l.get(1), CreditDebitCode.DBIT, "80.00", "7408409", null, "Motpart 2 AB");
		assertTx(l.get(2), CreditDebitCode.DBIT, "518.00", "9315382", null, "Motpart 2 AB");
		assertTx(l.get(3), CreditDebitCode.DBIT, "361.25", "4193457", null, "Motpart 3 AB");
		assertTx(l.get(4), CreditDebitCode.DBIT, "1120.00", "1465129", null, "Motpart 4 AB");
		assertTx(l.get(5), CreditDebitCode.DBIT, "21476.00", "8938392", null, "Motpart 5 AB");
	}

	/**
	 * Incoming payments in a batch. The payer's EndToEndId ("Testbolaget",
	 * "75225015-CAB-...") must not be used as invoice number.
	 */
	@Test
	public void incomingPaymentsWithPayerEndToEndId() throws Exception {
		List<Tx> l = resolveAll("GEUT_CAMTKRED_20261104_215813.TEST_inbet.xml");
		assertEquals(3, l.size());
		assertTx(l.get(0), CreditDebitCode.CRDT, "2615.00", "911035", "9110354", "MOTPART 6 AB");
		// Refund without OCR, only a referred document number
		assertTx(l.get(1), CreditDebitCode.CRDT, "1120.00", "2697130 8088105098064  Dubbelbetaln", null, "Motpart 4 AB");
		// No EndToEndId at all
		assertTx(l.get(2), CreditDebitCode.CRDT, "4288.00", "268579", "2685790", "Motpart 7 AB");
	}

	/**
	 * Batch with five transactions, mixed with and without EndToEndId.
	 */
	@Test
	public void incomingPaymentsBatch() throws Exception {
		List<Tx> l = resolveAll("GEUT_CAMTKRED_20261109_220257.TEST_inbet.xml");
		assertEquals(6, l.size());
		assertTx(l.get(0), CreditDebitCode.CRDT, "6906.00", "394180", "3941804", "MOTPART 8 AB");
		assertTx(l.get(1), CreditDebitCode.CRDT, "19886.00", "910026", "9100264", "MOTPART 9 AB");
		assertTx(l.get(2), CreditDebitCode.CRDT, "3360.00", "953895", "9538950", "MOTPART 10 AB");
		assertTx(l.get(3), CreditDebitCode.CRDT, "4654.00", "361649", "3616497", "MOTPART 11 AB");
		assertTx(l.get(4), CreditDebitCode.CRDT, "8515.00", "512495", "5124953", "MOTPART 12 AB");
		assertTx(l.get(5), CreditDebitCode.CRDT, "4669.00", "938373", "9383738", "Motpart 13 AB");
	}

	@Test
	public void incomingPaymentSingle() throws Exception {
		List<Tx> l = resolveAll("GEUT_CAMTKRED_20261110_215851.TEST_inbet.xml");
		assertEquals(1, l.size());
		assertTx(l.get(0), CreditDebitCode.CRDT, "10774.00", "815550", "8155509", "Motpart 14 AB");
	}

	/**
	 * Old format (2022, pretty printed, no Dbtr) must still work.
	 */
	@Test
	public void outgoingPayments2022() throws Exception {
		List<Tx> l = resolveAll("GEUT_CAMTDEB_20220318_220247.XML");
		assertEquals(30, l.size());
		assertTx(l.get(0), CreditDebitCode.DBIT, "16000", "1264159", null, null);
		assertTx(l.get(15), CreditDebitCode.DBIT, "125000", "1264153", null, null);
		assertTx(l.get(29), CreditDebitCode.DBIT, "24475.5", "220315-163725-366785", null, "RESPONSE NORDIC AB");
	}

	/**
	 * Incoming payment without remittance information: fall back on EndToEndId.
	 */
	@Test
	public void incomingWithoutRemittanceFallsBackOnEndToEndId() {
		EntryTransaction2 t = new EntryTransaction2();
		TransactionReferences2 refs = new TransactionReferences2();
		refs.setEndToEndId(" 12345 ");
		t.setRefs(refs);
		CAMT54ReferenceResolver.Result r = CAMT54ReferenceResolver.resolve(t, CreditDebitCode.CRDT);
		assertEquals("12345", r.invoiceNo);
		assertNull(r.paymentReference);
		assertNull(r.counterpartyName);
	}

	/**
	 * Incoming payment with only unstructured remittance information.
	 */
	@Test
	public void incomingWithUnstructuredRemittance() {
		EntryTransaction2 t = new EntryTransaction2();
		TransactionReferences2 refs = new TransactionReferences2();
		refs.setEndToEndId("PAYER-REF-1");
		t.setRefs(refs);
		RemittanceInformation5 rmt = new RemittanceInformation5();
		rmt.getUstrd().add("  ");
		rmt.getUstrd().add("Faktura 4711");
		t.setRmtInf(rmt);
		CAMT54ReferenceResolver.Result r = CAMT54ReferenceResolver.resolve(t, CreditDebitCode.CRDT);
		assertEquals("Faktura 4711", r.invoiceNo);
	}

	@Test
	public void emptyTransaction() {
		CAMT54ReferenceResolver.Result r = CAMT54ReferenceResolver.resolve(new EntryTransaction2(), CreditDebitCode.DBIT);
		assertNull(r.invoiceNo);
		assertNull(r.paymentReference);
	}

}
