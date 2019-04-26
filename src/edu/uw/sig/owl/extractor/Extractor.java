/**
 * 
 */
package edu.uw.sig.owl.extractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentTarget;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLProperty;
import org.semanticweb.owlapi.model.OWLPropertyAxiom;
import org.semanticweb.owlapi.model.OWLPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyAxiom;
import org.semanticweb.owlapi.model.UnloadableImportException;
import org.semanticweb.owlapi.util.AutoIRIMapper;

//import edu.washington.sig.view.PartitionFromISAGenerator;

/**
 * @author detwiler
 * @date Oct 27, 2014
 *
 */
public class Extractor
{
	private OWLOntologyManager sourceOntMan;
	private OWLDataFactory sourceOntDF;
	private OWLOntology sourceOnt;
	
	private OWLOntologyManager extOntMan;
	private OWLDataFactory extOntDF;
	private OWLOntology extOnt;
	
	private String rootsPath;
	private String propsPath;
	private String extPropNames;
	private String sourceIRIBase;
	private String extIRIBase;
	
	//private PartitionFromISAGenerator viewGen;
	private Properties configFile;
	
	private Set<OWLClass> roots;
	private Set<OWLClassExpression> extClasses = new HashSet<OWLClassExpression>();
	
	// properties to keep
	private Set<OWLProperty> extProps = new HashSet<OWLProperty>();
	private Set<OWLAnnotationProperty> extAnnotProps = new HashSet<OWLAnnotationProperty>();
	
	private boolean init(String baseOntPath, String extOntIRI) throws OWLOntologyCreationException, IOException
	{
		// first load base ontology
		sourceOntMan = OWLManager.createOWLOntologyManager();
		
		// deal with imports
		AutoIRIMapper mapper = new AutoIRIMapper(new File("resource"), true);
		sourceOntMan.addIRIMapper(mapper);
		
		sourceOntDF = sourceOntMan.getOWLDataFactory();
		
		File baseFile = new File(baseOntPath);
		try
		{
			sourceOnt = sourceOntMan.loadOntologyFromOntologyDocument(baseFile);
		}
		catch (UnloadableImportException e)
		{
			return false;
		}
		
		// now create the new extracted ontology
		extOntMan = OWLManager.createOWLOntologyManager();
		extOntMan.addIRIMapper(mapper);
		extOntDF = extOntMan.getOWLDataFactory();
		extOnt = extOntMan.createOntology(IRI.create(extOntIRI));
		/*
		File extFile = new File(extOntPath);
		extFile.createNewFile();
		extOnt = extOntMan.createOntology(IRI.create(extFile));//.loadOntologyFromOntologyDocument(extFile);
		*/
		
		
		
		configFile = new Properties();
	    try
		{
			configFile.load(this.getClass().getResourceAsStream("view.properties"));
		}
		catch (IOException e)
		{
			return false;
		}
	    
	    // make sure that all config properties are present
    	if(!configFile.containsKey("ROOTS")||
    			!configFile.containsKey("PROPERTIES")||
    			!configFile.containsKey("EXT_PROP_NAMES")||
    			!configFile.containsKey("SOURCE_IRI_BASE")||
    			!configFile.containsKey("EXTRACTION_IRI_BASE"))
    	{
    		System.err.println("View Generator is not properly configured");
    		return false;
    	}
		
		rootsPath = configFile.getProperty("ROOTS");
		propsPath = configFile.getProperty("PROPERTIES");
		extPropNames = configFile.getProperty("EXT_PROP_NAMES");
		sourceIRIBase = configFile.getProperty("SOURCE_IRI_BASE");
		extIRIBase = configFile.getProperty("EXTRACTION_IRI_BASE");
		
		getIsaRoots(rootsPath);
		populateExtProperties(propsPath);
		
		return true;
	}
	
	/*
	public void run()
	{
		for(OWLClass root : roots)
		{
			Set<OWLClass> rootSubs = getAllSubclasses(root);
			System.out.println("subs of "+root+": "+rootSubs);
		}
	}
	*/
	
	private void getIsaRoots(String rootsFileName)
	{
		Map<String,OWLClass> label2ClassMap = new HashMap<String,OWLClass>();
		Set<OWLClass> classes = sourceOnt.getClassesInSignature();
		for(OWLClass currClass : classes)
		{
			Set<OWLAnnotation> labels = currClass.getAnnotations(sourceOnt,sourceOntDF.getRDFSLabel());
			for(OWLAnnotation label : labels)
			{
				OWLLiteral labelAnnotLiteral = (OWLLiteral)label.getValue();
				String name = labelAnnotLiteral.getLiteral();
				label2ClassMap.put(name, currClass);
			}
		}
		
		roots = new HashSet<OWLClass>();
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(rootsFileName));
			String line = null;
	        while ((line=reader.readLine()) != null) 
	        {
	        	OWLClass currRootClass = label2ClassMap.get(line);
	        	if(currRootClass==null)
	        	{
	        		System.err.println("No class found for root "+line);
	        	}
	        	else
	        		roots.add(currRootClass);
	        }
		}
		catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private Set<OWLClass> getAllSubclasses(OWLClass root)
	{
		Set<OWLClass> subclasses = new HashSet<OWLClass>();
		Set<OWLSubClassOfAxiom> subAxioms = sourceOnt.getSubClassAxiomsForSuperClass(root);
		for(OWLSubClassOfAxiom subAxiom : subAxioms)
		{
			OWLClassExpression currSubClassExpr = subAxiom.getSubClass();
			if(currSubClassExpr instanceof OWLClass)
			{
				OWLClass currSubClass = currSubClassExpr.asOWLClass();
				subclasses.add(currSubClass);
				subclasses.addAll(getAllSubclasses(currSubClass));
			}
		}
		
		// test 
		/*
		System.err.println(owlClassSetAsString(new HashSet<OWLClass>(Collections.singletonList(root)))+" has subclasses "+
				owlClassSetAsString(subclasses));
				*/
		
		return subclasses;
	}
	
	private Set<OWLClass> getAllClassesByExistRestr(OWLClass root, Set<OWLObjectProperty> properties)
	{
		Set<OWLClass> relclasses = new HashSet<OWLClass>();
		Set<OWLSubClassOfAxiom> subAxioms = sourceOnt.getSubClassAxiomsForSubClass(root);
		for(OWLSubClassOfAxiom subAxiom : subAxioms)
		{
			OWLClassExpression currSuperClassExpr = subAxiom.getSuperClass();
			if(currSuperClassExpr instanceof OWLObjectSomeValuesFrom)
			{
				OWLObjectSomeValuesFrom restr = (OWLObjectSomeValuesFrom)currSuperClassExpr;
				OWLClassExpression restrFiller = restr.getFiller();
				OWLObjectPropertyExpression prop = restr.getProperty();
				if(!properties.contains(prop))
					continue;
				if(restrFiller instanceof OWLClass)
				{
					OWLClass currFiller = restrFiller.asOWLClass();
					
					//System.err.println("adding class to seed: "+owlClassAsString(currFiller));
					
					relclasses.add(currFiller);
					relclasses.addAll(getAllClassesByExistRestr(currFiller,properties));
				}
			}
		}
		
		return relclasses;
	}
	
	private Set<OWLClassExpression> getAllSuperclassExprs(OWLClass sub)
	{
		Set<OWLClassExpression> superclasses = new HashSet<OWLClassExpression>();
		Set<OWLSubClassOfAxiom> subAxioms = sourceOnt.getSubClassAxiomsForSubClass(sub);
		for(OWLSubClassOfAxiom subAxiom : subAxioms)
		{
			OWLClassExpression currSuperClassExpr = subAxiom.getSuperClass();
			
			if(currSuperClassExpr instanceof OWLClass)
			{
				OWLClass currSuperClass = currSuperClassExpr.asOWLClass();
				superclasses.add(currSuperClass);
				superclasses.addAll(getAllSuperclassExprs(currSuperClass));
			}
		}
		
		return superclasses;
	}
	
	private void populateExtProperties(String propFileName)
	{
		// if path is null, default to keeping all properties
		if(propFileName==null)
		{
			extProps.addAll(sourceOnt.getObjectPropertiesInSignature());
			extProps.addAll(sourceOnt.getDataPropertiesInSignature());
			extAnnotProps.addAll(sourceOnt.getAnnotationPropertiesInSignature());
			return;
		}
		
		// if we made it here, there is a restricted property list to consider
		Set<IRI> propIRIs = new HashSet<IRI>();
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(propFileName));
			String line = null;
	        while ((line=reader.readLine()) != null) 
	        {
	        	IRI currPropIRI = IRI.create(line);
	        	if(currPropIRI!=null)
	        		propIRIs.add(currPropIRI);
	        }
		}
		catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// check data properties
		for(OWLObjectProperty objProp : sourceOnt.getObjectPropertiesInSignature())
		{
			if(propIRIs.contains(objProp.getIRI()))
				extProps.add(objProp);
		}
		
		// check data properties
		for(OWLDataProperty dataProp : sourceOnt.getDataPropertiesInSignature())
		{
			if(propIRIs.contains(dataProp.getIRI()))
				extProps.add(dataProp);
		}
		
		// check annotation properties
		for(OWLAnnotationProperty annotProp : sourceOnt.getAnnotationPropertiesInSignature())
		{
			if(propIRIs.contains(annotProp.getIRI()))
				extAnnotProps.add(annotProp);
		}
	}
	
	private Set<OWLObjectProperty> getExtSeedProps()
	{
		Set<OWLObjectProperty> returnProps = new HashSet<OWLObjectProperty>();
		
		for(String propName : extPropNames.split("\\|"))
		{
			OWLObjectProperty currProp = sourceOntDF.getOWLObjectProperty(IRI.create(propName));
			if(currProp==null)
				System.err.println("ERROR: no property found for "+propName);
			else
			{
				returnProps.add(currProp);
			}
		}
		
		
		return returnProps;
	}
	
	public void run(String baseOntPath, String extOntPath, String extOntIRI)
	{
		try
		{
			if(!init(baseOntPath, extOntIRI))
			{
				throw new OWLOntologyCreationException("init failed");
			}
		}
		catch (OWLOntologyCreationException e)
		{
			e.printStackTrace();
			return;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return;
		}
		
		// begin extraction
		
		// create set of seed classes
		Set<OWLClass> seeds = new HashSet<OWLClass>();
		
		// add all of the original roots to the set of seed classes
		seeds.addAll(roots);
		
		for(OWLClass root : roots)
		{
			/*
			 * This code is for using properties other than subclass to build extraction seeds
			 * 
			Set<OWLClass> rootSubs = getAllClassesByExistRestr(root, extProps);
			
			// add all subclasses to set of seed classes
			seeds.addAll(rootSubs);
			*/
			
			Set<OWLClass> rootSubs = getAllSubclasses(root);
			
			// add all subclasses to set of seed classes
			seeds.addAll(rootSubs);
		}
		
		// populate set of class expressions in extraction
		extClasses.addAll(seeds);
		for(OWLClass seed : seeds)
		{
			Set<OWLClassExpression> seedSupers = getAllSuperclassExprs(seed);
			
			// add all superclasses to set extraction classes
			extClasses.addAll(seedSupers);
		}
		
		// gather all axioms that have an extraction class as the source
		for(OWLClassExpression extClassExpr : extClasses)
		{
			if(extClassExpr instanceof OWLClass)
			{
				OWLClass extClass = extClassExpr.asOWLClass();
				
				copyClassAxioms(extClass);				
				copyClassAnnotations(extClass);
			}
		}
		
		// gather all extraction property info
		for(OWLProperty currProp : extProps)
		{
			copyPropertyAxioms(currProp);
			copyPropertyDomain(currProp);
			copyPropertyRange(currProp);
			copyPropertyInverse(currProp);
			copySubPropertyAxioms(currProp);
			copyPropertyAnnotations(currProp);
			//copyPropertyCharacteristics(currProp);
			//TODO copy property facets, inverses, subproperty
			
		}
		
		// also handle annotation properties
		for(OWLAnnotationProperty currAnnotProp : extAnnotProps)
		{
			copyPropertyAxioms(currAnnotProp);
			copySubPropertyAxioms(currAnnotProp);
			copyPropertyAnnotations(currAnnotProp);
		}
		
		saveExtraction(extOntPath);
	}

	private void copyClassAxioms(OWLClass extClass)
	{
		// define class
		//factory.getOWLClass(IRI.create(input))
		OWLClass newExtClass = extOntDF.getOWLClass(extClass.getIRI());
		OWLAxiom declAx = extOntDF.getOWLDeclarationAxiom(newExtClass);
		extOntMan.applyChange(new AddAxiom(extOnt, declAx));

		// copy axioms
		Set<OWLClassAxiom> classAxioms = sourceOnt.getAxioms(extClass);
		for(OWLClassAxiom classAxiom : classAxioms)
		{
			// test for viability of axiom
			// assumes all axioms are either subclass or disjoint classes axioms
			if(classAxiom instanceof OWLDisjointClassesAxiom)
			{
				//OWLDisjointClassesAxiom disAxiom = (OWLDisjointClassesAxiom)classAxiom;
				Set<OWLClass> disClasses = classAxiom.getClassesInSignature();
				Set<OWLClass> reducedSet = new HashSet<OWLClass>(disClasses);
				reducedSet.retainAll(extClasses);
				
				if(reducedSet.isEmpty()||reducedSet.size()==1)
					continue;
				
				classAxiom = extOntDF.getOWLDisjointClassesAxiom(reducedSet);
			}
			else if(classAxiom instanceof OWLSubClassOfAxiom)
			{
				Set<OWLClass> axSigClasses = classAxiom.getClassesInSignature();
				boolean usableInView = true;
				for(OWLClass axSigClass : axSigClasses)
				{
					if(!extClasses.contains(axSigClass))
					{
						//System.err.println("axiom rejected due to reference to class: "+getFirstSourceClassLabel(axSigClass));
						usableInView = false;
						break;
					}
				}
				
				Set<OWLProperty> axSigProps = new HashSet<OWLProperty>();
				axSigProps.addAll(classAxiom.getDataPropertiesInSignature());
				axSigProps.addAll(classAxiom.getObjectPropertiesInSignature());
				for(OWLProperty axSigProp : axSigProps)
				{
					if(!extProps.contains(axSigProp))
					{
						System.err.println("axiom rejected due to reference to property: "+axSigProp);
						usableInView = false;
						break;
					}
				}
				
				if(!usableInView)
					continue;
			}
			else
			{
				System.err.println("unsupported class axiom type: "+classAxiom);
				continue;
			}
			
			// test build of extraction ontology
			//System.err.println("about to add: "+classAxiom);
			AddAxiom addAxiom1 = new AddAxiom(extOnt, classAxiom);
			extOntMan.applyChange(addAxiom1);
		}
	}
	
	private void copyClassAnnotations(OWLClass extClass)
	{
		// copy annotations
		Set<OWLAnnotationAssertionAxiom> classAnnots = extClass.getAnnotationAssertionAxioms(sourceOnt);
		for(OWLAnnotationAssertionAxiom classAnnot : classAnnots)
		{
			OWLAnnotationProperty annotProp = classAnnot.getProperty();
			if(!extAnnotProps.contains(annotProp))
			{
				System.err.println("axiom rejected due to reference to property: "+annotProp);
				continue;
			}
			
			// add the new axiom to the extraction
			AddAxiom addAxiom1 = new AddAxiom(extOnt, classAnnot);
			extOntMan.applyChange(addAxiom1);
		}
	}
	
	private void copyPropertyAxioms(OWLEntity extProp)
	{
		/* done elsewhere
		// do nothing if property is not in extraction
		if(!extProps.contains(extProp))
			return;
			*/
		
		// copy property definition
		OWLEntity newExtProperty = null;
		Set<OWLAxiom> propAxioms = new HashSet<OWLAxiom>();
		if(extProp instanceof OWLDataProperty)
		{
			OWLDataProperty extDataProp = (OWLDataProperty) extProp;
			newExtProperty = extOntDF.getOWLDataProperty(extDataProp.getIRI());
			propAxioms.addAll(sourceOnt.getAxioms(extDataProp));
		}
		else if(extProp instanceof OWLObjectProperty)
		{
			OWLObjectProperty extObjProp = (OWLObjectProperty) extProp;
			newExtProperty = extOntDF.getOWLObjectProperty(extObjProp.getIRI());
			propAxioms.addAll(sourceOnt.getAxioms(extObjProp));
		}
		else if(extProp instanceof OWLAnnotationProperty)
		{
			OWLAnnotationProperty extAnnotProp = (OWLAnnotationProperty) extProp;
			newExtProperty = extOntDF.getOWLAnnotationProperty(extAnnotProp.getIRI());
			propAxioms.addAll(sourceOnt.getAxioms(extAnnotProp));
		}
		
		OWLAxiom declAx = extOntDF.getOWLDeclarationAxiom(newExtProperty);
		extOntMan.applyChange(new AddAxiom(extOnt, declAx));
		
		//-----
		for(OWLAxiom propAxiom : propAxioms)
		{
			// test for viability of axiom
			
			// because all axioms that are either sub-property, domain, range, or inverse must make checks
			// to verify that they belong in the extraction, they are handled elsewhere. I believe that this
			// leaves only those axiom that describe facets of a property (i.e. symmetric, functional, etc.)
			if(propAxiom instanceof OWLPropertyDomainAxiom)
			{
				// ignore, handled elsewhere
				continue;
			}
			else if(propAxiom instanceof OWLPropertyRangeAxiom)
			{
				// ignore, handled elsewhere
				continue;
			}
			else if(propAxiom instanceof OWLSubPropertyAxiom)
			{
				// ignore, handled elsewhere
				continue;
			}
			else if(propAxiom instanceof OWLInverseObjectPropertiesAxiom)
			{
				// ignore, handled elsewhere
				continue;
			}
			
			extOntMan.applyChange(new AddAxiom(extOnt, propAxiom));
		}
		
		
		//-----


		// copy axioms
		/*
		for(OWLAxiom propAxiom : propAxioms)
		{
			extOntMan.applyChange(new AddAxiom(extOnt, propAxiom));
		}
		*/
	}
	
	private void copyPropertyDomain(OWLProperty extProp)
	{
		Set<OWLClassExpression> currPropDomainExprs = extProp.getDomains(sourceOnt);
		for(OWLClassExpression currClassExpr : currPropDomainExprs)
		{
			if(currClassExpr instanceof OWLClass)
			{
				OWLClass currClass = currClassExpr.asOWLClass();
				if(extClasses.contains(currClass))
				{
					OWLPropertyDomainAxiom domainAx = null;
					// domain class is in extraction, add domain assertion
					if(extProp.isOWLObjectProperty())
						domainAx = extOntDF.getOWLObjectPropertyDomainAxiom(extProp.asOWLObjectProperty(), currClass);
					else if(extProp.isOWLDataProperty())
						domainAx = extOntDF.getOWLDataPropertyDomainAxiom(extProp.asOWLDataProperty(), currClass);
					else
					{
						System.err.println("Invalid property type found");
					}
					
					if(domainAx!=null)
					{
						AddAxiom addDomainAx = new AddAxiom(extOnt, domainAx);
						extOntMan.applyChange(addDomainAx);
					}
				}
			}
			else if(currClassExpr instanceof OWLObjectUnionOf)
			{
				Set<OWLClassExpression> currUnion = currClassExpr.asDisjunctSet();
				currUnion.retainAll(extClasses);
				if(!currUnion.isEmpty())
				{
					OWLObjectUnionOf newUnion = extOntDF.getOWLObjectUnionOf(currUnion);
					
					OWLPropertyDomainAxiom domainAx = null;
					if(extProp.isOWLObjectProperty())
						domainAx = extOntDF.getOWLObjectPropertyDomainAxiom(extProp.asOWLObjectProperty(), newUnion);
					else if(extProp.isOWLDataProperty())
						domainAx = extOntDF.getOWLDataPropertyDomainAxiom(extProp.asOWLDataProperty(), newUnion);
					else
					{
						System.err.println("Invalid property type found");
					}
					
					if(domainAx!=null)
					{
						AddAxiom addDomainAx = new AddAxiom(extOnt, domainAx);
						extOntMan.applyChange(addDomainAx);
					}
				}
			}
			else
			{
				System.err.println("Unsupported property domain expression: "+currClassExpr);
			}
		}
	}
	
	private void copyPropertyRange(OWLProperty extProp)
	{
		if(extProp.isOWLObjectProperty())
		{
			Set<OWLClassExpression> currPropRangeExprs = extProp.getRanges(sourceOnt);
			for(OWLClassExpression currClassExpr : currPropRangeExprs)
			{
				if(currClassExpr instanceof OWLClass)
				{
					OWLClass currClass = currClassExpr.asOWLClass();
					if(extClasses.contains(currClass))
					{
						// domain class is in extraction, add domain assertion
						OWLPropertyRangeAxiom rangeAx = extOntDF.getOWLObjectPropertyRangeAxiom(extProp.asOWLObjectProperty(), currClass);
						
						if(rangeAx!=null)
						{
							AddAxiom addRangeAx = new AddAxiom(extOnt, rangeAx);
							extOntMan.applyChange(addRangeAx);
						}
					}
				}
				else if(currClassExpr instanceof OWLObjectUnionOf)
				{
					Set<OWLClassExpression> currUnion = currClassExpr.asDisjunctSet();
					currUnion.retainAll(extClasses);
					if(!currUnion.isEmpty())
					{
						OWLObjectUnionOf newUnion = extOntDF.getOWLObjectUnionOf(currUnion);
						
						OWLPropertyRangeAxiom rangeAx = extOntDF.getOWLObjectPropertyRangeAxiom(extProp.asOWLObjectProperty(), newUnion);
						
						if(rangeAx!=null)
						{
							AddAxiom addRangeAx = new AddAxiom(extOnt, rangeAx);
							extOntMan.applyChange(addRangeAx);
						}
					}
				}
				else
				{
					System.err.println("Unsupported property range expression: "+currClassExpr);
				}
			}
		
		}
		else if(extProp.isOWLDataProperty())
		{
			Set<OWLDataRange> dataPropertyRanges = extProp.getRanges(sourceOnt);
			
			for(OWLDataRange currDataRange : dataPropertyRanges)
			{
				OWLPropertyRangeAxiom rangeAx = extOntDF.getOWLDataPropertyRangeAxiom(extProp.asOWLDataProperty(),  currDataRange);
				
				if(rangeAx!=null)
				{
					AddAxiom addRangeAx = new AddAxiom(extOnt, rangeAx);
					extOntMan.applyChange(addRangeAx);
				}
			}
			
		}
		else
		{
			System.err.println("unsupported property type found: "+extProp);
		}
	}
	
	private void copyPropertyInverse(OWLProperty currProp)
	{
		if(!currProp.isObjectPropertyExpression()) //only object properties will have inverses
			return;
		
		OWLObjectPropertyExpression invProp = currProp.asOWLObjectProperty().getInverseProperty();
		if(invProp==null || !extProps.contains(invProp))
			return;
		
		OWLAxiom invAx = extOntDF.getOWLInverseObjectPropertiesAxiom(currProp.asOWLObjectProperty(), invProp);
		
		AddAxiom addInvAx = new AddAxiom(extOnt, invAx);
		extOntMan.applyChange(addInvAx);
	}

	private void copySubPropertyAxioms(OWLEntity currProp)
	{
		//OWLAxiom subPropAxiom = null;
		if(currProp instanceof OWLDataProperty)
		{
			OWLDataProperty currDataProp = currProp.asOWLDataProperty();
			Set<OWLDataPropertyExpression> superProps = currDataProp.getSuperProperties(sourceOnt);
			for(OWLDataPropertyExpression superProp : superProps)
			{
				if(!extProps.contains(superProp)) // check to see if super property is in extension
					continue;
				
				OWLAxiom subPropAx = extOntDF.getOWLSubDataPropertyOfAxiom(currDataProp, superProp);
				AddAxiom addSubPropAx = new AddAxiom(extOnt, subPropAx);
				extOntMan.applyChange(addSubPropAx);
			}
		}
		else if(currProp instanceof OWLObjectProperty)
		{
			OWLObjectProperty currObjectProp = currProp.asOWLObjectProperty();
			Set<OWLObjectPropertyExpression> superProps = currObjectProp.getSuperProperties(sourceOnt);
			for(OWLObjectPropertyExpression superProp : superProps)
			{
				if(!extProps.contains(superProp)) // check to see if super property is in extension
					continue;
				
				OWLAxiom subPropAx = extOntDF.getOWLSubObjectPropertyOfAxiom(currObjectProp, superProp);
				AddAxiom addSubPropAx = new AddAxiom(extOnt, subPropAx);
				extOntMan.applyChange(addSubPropAx);
			}
		}
		else if(currProp instanceof OWLAnnotationProperty)
		{
			OWLAnnotationProperty currAnnotProp = currProp.asOWLAnnotationProperty();
			Set<OWLAnnotationProperty> superProps = currAnnotProp.getSuperProperties(sourceOnt);
			for(OWLAnnotationProperty superProp : superProps)
			{
				if(!extProps.contains(superProp)) // check to see if super property is in extension
					continue;
				
				OWLAxiom subPropAx = extOntDF.getOWLSubAnnotationPropertyOfAxiom(currAnnotProp, superProp);
				AddAxiom addSubPropAx = new AddAxiom(extOnt, subPropAx);
				extOntMan.applyChange(addSubPropAx);
			}
		}
		
	}

	private void copyPropertyAnnotations(OWLEntity currProp)
	{
		// NOTE! This assumes annotations are primitives (string, int, float, etc.) not objects (class, individual, etc.)
		// object values will be copied, but they will not be checked to see if they belong in the extraction
		Set<OWLAnnotationAssertionAxiom> annotAssertAxioms = currProp.getAnnotationAssertionAxioms(sourceOnt);
		
		for(OWLAnnotationAssertionAxiom currAnnotAssertAxiom : annotAssertAxioms)
		{
			// add the axioms to the ontology.
			AddAxiom addAxiom = new AddAxiom(extOnt, currAnnotAssertAxiom);
			
			// We now use the manager to apply the change
			extOntMan.applyChange(addAxiom);
		}
	}
	
	/*
	private void copyPropertyAnnotations(OWLProperty currProp)
	{
		// first copy property annotations 
		// NOTE! This assumes annotations are primitives (string, int, float, etc.) not objects (class, individual, etc.)
		// object values will be copied, but they will not be checked to see if they belong in the extraction
		Set<OWLAnnotationAssertionAxiom> annotAssertAxioms = currProp.getAnnotationAssertionAxioms(sourceOnt);
		
		for(OWLAnnotationAssertionAxiom currAnnotAssertAxiom : annotAssertAxioms)
		{
			// add the axioms to the ontology.
			AddAxiom addAxiom = new AddAxiom(extOnt, currAnnotAssertAxiom);
			
			// We now use the manager to apply the change
			extOntMan.applyChange(addAxiom);
		}
	}
	*/
	
	private String owlClassSetAsString(Set<OWLClassExpression> classes)
	{
		OWLClassExpression[] classArray = classes.toArray(new OWLClassExpression[classes.size()]);
		StringBuffer classListSB = new StringBuffer();
		classListSB.append("[");
		for(int counter=0; counter<classArray.length; counter++)
		{
			OWLClassExpression currClassExp = classArray[counter];
			if(currClassExp instanceof OWLClass)
			{
				OWLClass currClass = currClassExp.asOWLClass();
				Set<OWLAnnotation> labels = currClass.getAnnotations(sourceOnt,sourceOntDF.getRDFSLabel());
				for(OWLAnnotation label : labels)
				{
					OWLLiteral labelAnnotLiteral = (OWLLiteral)label.getValue();
					String name = labelAnnotLiteral.getLiteral();
					classListSB.append(name);
					if(counter!=classArray.length-1)
						classListSB.append(", ");
				}
			}
			else
			{
				classListSB.append(currClassExp.toString());
			}
		}
		classListSB.append("]");
		return classListSB.toString();
	}
	
	private String owlClassAsString(OWLClassExpression currClassExp)
	{
		StringBuffer classListSB = new StringBuffer();

		if(currClassExp instanceof OWLClass)
		{
			OWLClass currClass = currClassExp.asOWLClass();
			Set<OWLAnnotation> labels = currClass.getAnnotations(sourceOnt,sourceOntDF.getRDFSLabel());
			for(OWLAnnotation label : labels)
			{
				OWLLiteral labelAnnotLiteral = (OWLLiteral)label.getValue();
				String name = labelAnnotLiteral.getLiteral();
				classListSB.append(name);
			}
		}
		else
		{
			classListSB.append(currClassExp.toString());
		}
		return classListSB.toString();
	}
	
	private boolean saveExtraction(String extOntPath)
	{
		//TODO: remove argument to this method
		try
		{
			File output = new File(extOntPath);
			//IRI documentIRI = IRI.create(output);
			
			 // Now save a copy to another location in OWL/XML format (i.e. disregard
			// the format that the ontology was loaded in).
			//File f = File.createTempFile("owlapiexample", "example1.xml");
			//IRI documentIRI2 = IRI.create(output);
			RDFXMLOntologyFormat format = new RDFXMLOntologyFormat();
			format.setDefaultPrefix(sourceIRIBase);
			extOntMan.saveOntology(extOnt, format, new FileDocumentTarget(output));
			
			// Remove the ontology from the manager
			extOntMan.removeOntology(extOnt);
			
			//output.delete(); 
		}
		catch (OWLOntologyStorageException e)
		{
			e.printStackTrace();
			return false;
		}
		/*
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
		*/
		
		return true;
	}
	
	/**
	 * This is just an extraction debugging method
	 */
	private void printLabelsOfUndefinedClasses()
	{
		Set<OWLEntity> extSig = extOnt.getSignature();
		for(OWLEntity currSigEnt : extSig)
		{
			if(currSigEnt instanceof OWLClass)
			{
				OWLClass currSigClass = currSigEnt.asOWLClass();
				
				// look up the label of this class in the source ontology, print it out
				OWLClass correspondingSourceClass = sourceOntDF.getOWLClass(currSigClass.getIRI());
				if(extClasses.contains(correspondingSourceClass)) // filter out classes that will be defined
					continue;
				Set<OWLAnnotation> sourceLabels = correspondingSourceClass.getAnnotations(sourceOnt, sourceOntDF.getRDFSLabel());
				for(OWLAnnotation sourceLabel : sourceLabels)
				{
					OWLLiteral labelAnnotLiteral = (OWLLiteral)sourceLabel.getValue();
					String name = labelAnnotLiteral.getLiteral();
					System.err.println("undefined class: "+name);
				}
			}
		}
	}
	
	/**
	 * This is just an extraction debugging method
	 */
	private void printLabelsOfSeedClasses()
	{
		Set<OWLEntity> extSig = extOnt.getSignature();
		for(OWLEntity currSigEnt : extSig)
		{
			if(currSigEnt instanceof OWLClass)
			{
				OWLClass currSigClass = currSigEnt.asOWLClass();
				
				// look up the label of this class in the source ontology, print it out
				OWLClass correspondingSourceClass = sourceOntDF.getOWLClass(currSigClass.getIRI());
				if(!extClasses.contains(correspondingSourceClass)) // filter out classes that will be defined
					continue;
				Set<OWLAnnotation> sourceLabels = correspondingSourceClass.getAnnotations(sourceOnt, sourceOntDF.getRDFSLabel());
				for(OWLAnnotation sourceLabel : sourceLabels)
				{
					OWLLiteral labelAnnotLiteral = (OWLLiteral)sourceLabel.getValue();
					String name = labelAnnotLiteral.getLiteral();
					System.err.println("seed class: "+name);
				}
			}
		}
	}
	
	private String getFirstSourceClassLabel(OWLClass currClass)
	{
		String label = "";
		Set<OWLAnnotation> sourceLabels = currClass.getAnnotations(sourceOnt, sourceOntDF.getRDFSLabel());
		for(OWLAnnotation sourceLabel : sourceLabels)
		{
			OWLLiteral labelAnnotLiteral = (OWLLiteral)sourceLabel.getValue();
			label = labelAnnotLiteral.getLiteral();	
		}
		return label;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		Extractor ext = new Extractor();
		ext.run(args[0],args[1],args[2]);
		//ext.printLabelsOfSeedClasses();
		//ext.printLabelsOfUndefinedClasses();
		
		/*
		try
		{
			ext.init(args[0],args[1]);
			ext.run();
		}
		catch (OWLOntologyCreationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

	}

}
