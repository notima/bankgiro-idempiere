package org.notima.idempiere.iso20022;

import java.util.List;

import iso.std.iso._20022.tech.xsd.camt_054_001.CreditDebitCode;
import iso.std.iso._20022.tech.xsd.camt_054_001.CreditorReferenceInformation2;
import iso.std.iso._20022.tech.xsd.camt_054_001.EntryTransaction2;
import iso.std.iso._20022.tech.xsd.camt_054_001.PartyIdentification32;
import iso.std.iso._20022.tech.xsd.camt_054_001.ProprietaryReference1;
import iso.std.iso._20022.tech.xsd.camt_054_001.ReferredDocumentInformation3;
import iso.std.iso._20022.tech.xsd.camt_054_001.RemittanceInformation5;
import iso.std.iso._20022.tech.xsd.camt_054_001.StructuredRemittanceInformation7;
import iso.std.iso._20022.tech.xsd.camt_054_001.TransactionParty2;
import iso.std.iso._20022.tech.xsd.camt_054_001.TransactionReferences2;

/**
 * Resolves which reference in a CAMT.054 transaction identifies our invoice.
 *
 * Has no iDempiere dependencies so it can be tested standalone.
 *
 * Outgoing payments (DBIT): EndToEndId is set by us in the PAIN-file
 * (our invoice document no) and is preferred.
 *
 * Incoming payments (CRDT): EndToEndId is set by the payer and can be anything
 * (Handelsbanken started sending it in 2026). The structured remittance
 * information (OCR / referred document) is therefore preferred, and
 * EndToEndId is only used as a last resort.
 */
public class CAMT54ReferenceResolver {

	public static class Result {
		/** Invoice number to look up (OCR without check digit if OCR) */
		public String invoiceNo;
		/** Full OCR reference, if the reference is an OCR (SCOR) */
		public String paymentReference;
		/** The counterparty's own reference, if any */
		public String bpInvoiceNo;
		/** Name of the counterparty (debtor for incoming, creditor for outgoing) */
		public String counterpartyName;
	}

	public static Result resolve(EntryTransaction2 ee, CreditDebitCode crDbCode) {

		Result r = new Result();
		boolean incoming = CreditDebitCode.CRDT.equals(crDbCode);

		TransactionReferences2 tr2 = ee.getRefs();
		String endToEndId = tr2!=null ? trimToNull(tr2.getEndToEndId()) : null;
		ProprietaryReference1 theirRef1 = tr2!=null ? tr2.getPrtry() : null;
		if (theirRef1!=null) {
			r.bpInvoiceNo = theirRef1.getRef();
		}

		if (!incoming && endToEndId!=null) {
			r.invoiceNo = endToEndId;
		} else {
			resolveFromRemittance(ee.getRmtInf(), r);
			if (r.invoiceNo==null) {
				r.invoiceNo = endToEndId;
			}
		}

		TransactionParty2 trp = ee.getRltdPties();
		if (trp!=null) {
			PartyIdentification32 pid = incoming ? trp.getDbtr() : trp.getCdtr();
			if (pid!=null) {
				r.counterpartyName = pid.getNm();
			}
		}

		return r;
	}

	private static void resolveFromRemittance(RemittanceInformation5 rmtInf, Result r) {

		if (rmtInf==null) return;

		// TODO: Allow for handling of more than one invoice in remittance list.
		for (StructuredRemittanceInformation7 s : rmtInf.getStrd()) {
			String ref = trimToNull(getMostLikelyReference(s));
			if (ref==null) continue;
			if (isOCRReference(s)) {
				r.paymentReference = ref;
				r.invoiceNo = ref.length()>1 ? ref.substring(0, ref.length()-1) : ref;
			} else {
				r.invoiceNo = ref;
			}
			return;
		}

		// Fall back on unstructured information
		List<String> ustrd = rmtInf.getUstrd();
		if (ustrd!=null) {
			for (String u : ustrd) {
				String ref = trimToNull(u);
				if (ref!=null) {
					r.invoiceNo = ref;
					return;
				}
			}
		}

	}

	public static String getMostLikelyReference(StructuredRemittanceInformation7 stri) {

		if (stri==null) return null;

		CreditorReferenceInformation2 cri = stri.getCdtrRefInf();
		if (cri!=null && trimToNull(cri.getRef())!=null) {
			return cri.getRef().trim();
		}

		List<ReferredDocumentInformation3> rlist = stri.getRfrdDocInf();
		if (rlist!=null) {
			for (ReferredDocumentInformation3 rdi : rlist) {
				if (trimToNull(rdi.getNb())!=null)
					return rdi.getNb().trim();
			}
		}

		return null;
	}

	public static boolean isOCRReference(StructuredRemittanceInformation7 sri) {
		if (sri==null) return false;
		if (sri.getCdtrRefInf()==null) return false;
		if (trimToNull(sri.getCdtrRefInf().getRef())==null) return false;
		if (sri.getCdtrRefInf().getTp()==null) return false;
		if (sri.getCdtrRefInf().getTp().getCdOrPrtry()==null) return false;
		if (sri.getCdtrRefInf().getTp().getCdOrPrtry().getCd()==null) return false;
		return ("SCOR".equals(sri.getCdtrRefInf().getTp().getCdOrPrtry().getCd().name()));
	}

	private static String trimToNull(String s) {
		if (s==null) return null;
		s = s.trim();
		return s.length()==0 ? null : s;
	}

}
