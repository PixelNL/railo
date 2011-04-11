package railo.transformer.library.tag;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.Type;

import railo.commons.lang.ClassException;
import railo.commons.lang.ClassUtil;
import railo.commons.lang.Md5;
import railo.commons.lang.StringUtil;
import railo.transformer.bytecode.cast.Cast;
import railo.transformer.bytecode.statement.tag.Attribute;
import railo.transformer.bytecode.statement.tag.Tag;
import railo.transformer.bytecode.statement.tag.TagBase;
import railo.transformer.cfml.attributes.AttributeEvaluator;
import railo.transformer.cfml.attributes.AttributeEvaluatorException;
import railo.transformer.cfml.evaluator.Evaluator;
import railo.transformer.cfml.evaluator.EvaluatorException;
import railo.transformer.cfml.tag.TagDependentBodyTransformer;

/**
 * Die Klasse TagLibTag rep�sentiert ein einzelne Tag Definition einer TagLib, 
 * beschreibt also alle Informationen die man zum validieren eines Tags braucht. 
 */
public final class TagLibTag {
	
	public final static int ATTRIBUTE_TYPE_FIXED=0;
	public final static int ATTRIBUTE_TYPE_DYNAMIC=1;
	public final static int ATTRIBUTE_TYPE_NONAME=3;
	public final static int ATTRIBUTE_TYPE_MIXED=4;
	
	
	/**
	 * Definition des Attribut Type 
	 */
	//public final static int ATTRIBUTE_TYPE_FULLDYNAMIC=2; deprecated
	/**
	 * Definition des Attribut Type 
	 */
	
	private final static Class[] CONSTRUCTOR_PARAMS=new Class[]{int.class,int.class};
	
	private int attributeType;
	private String name;
	private boolean hasBody=true;
	
	private boolean isBodyReq=false;
	private boolean isTagDependent=false;
	private boolean bodyFree=true;
	
	private boolean parseBody;
	private boolean hasAppendix;
	private String description="";
	private String tagClass;
	private String tteClass;
	private String tdbtClass;
	private int min;
	private int max;
	private TagLib tagLib;
	private Evaluator eval;
	private TagDependentBodyTransformer tdbt;

	private Map<String,TagLibTagAttr> attributes=new HashMap<String,TagLibTagAttr>();
	private Map<String,String> setters=new HashMap<String,String>();
	private TagLibTagAttr attrFirst;
	private TagLibTagAttr attrLast;
	
	private String strAttributeEvaluator;
	private AttributeEvaluator attributeEvaluator;
	private boolean handleException;
    private boolean hasDefaultValue=false;
	private Type tagType;
	private String tttClass;
	private Constructor  tttConstructor;
	private boolean allowRemovingLiteral;
	private TagLibTagAttr defaultAttribute;
	private short status=TagLib.STATUS_IMPLEMENTED;
	private Class clazz;
	private TagLibTagScript script;

	public TagLibTag duplicate(boolean cloneAttributes) {
		TagLibTag tlt = new TagLibTag(tagLib);

		tlt.attributeType=attributeType;
		tlt.name=name;
		tlt.hasBody=hasBody;
		tlt.isBodyReq=isBodyReq;
		tlt.isTagDependent=isTagDependent;
		tlt.bodyFree=bodyFree;
		tlt.parseBody=parseBody;
		tlt.hasAppendix=hasAppendix;
		tlt.description=description;
		tlt.tagClass=tagClass;
		tlt.tteClass=tteClass;
		tlt.tdbtClass=tdbtClass;
		tlt.min=min;
		tlt.max=max;
		tlt.strAttributeEvaluator=strAttributeEvaluator;
		tlt.handleException=handleException;
		tlt.hasDefaultValue=hasDefaultValue;
		tlt.tagType=tagType;
		tlt.tttClass=tttClass;
		tlt.tttConstructor=tttConstructor;
		tlt.allowRemovingLiteral=allowRemovingLiteral;
		tlt.status=status;
		
		tlt.eval=null;
		tlt.tdbt=null;
		tlt.attributeEvaluator=null;
		
		
		Iterator<Entry<String, TagLibTagAttr>> it = attributes.entrySet().iterator();
		if(cloneAttributes) {
			while(it.hasNext()){
				tlt.setAttribute(it.next().getValue().duplicate(tlt));
			}
			if(defaultAttribute!=null)tlt.defaultAttribute=defaultAttribute.duplicate(tlt);
		}
		else {
			while(it.hasNext()){
				tlt.setAttribute(it.next().getValue());
				tlt.attrFirst=attrFirst;
				tlt.attrLast=attrLast;
			}
			tlt.defaultAttribute=defaultAttribute;
		}
		
		
		// setter
		Iterator<Entry<String, String>> sit = setters.entrySet().iterator();
		Entry<String, String> se;
		while(sit.hasNext()){
			se = sit.next();
			tlt.setters.put(se.getKey(), se.getValue());
		}
		
		
		
/*
		private Map attributes=new HashMap();
		private TagLibTagAttr attrFirst;
		private TagLibTagAttr attrLast;
		
		private Map setters=new HashMap();
		private TagLibTagAttr defaultAttribute;
	*/	
		return tlt;
	}
	
	
	/**
	 * Gesch�tzer Konstruktor ohne Argumente.
	 * @param tagLib
	 */
	public TagLibTag(TagLib tagLib) {
	    this.tagLib=tagLib;
	}

	/**
	 * Gibt alle Attribute (TagLibTagAttr) eines Tag als HashMap zur�ck.
	 * @return HashMap Attribute als HashMap.
	 */
	public Map<String,TagLibTagAttr> getAttributes() {
		return attributes;
	}
	
	/**
	 * Gibt ein bestimmtes Attribut anhand seines Namens zur�ck, 
	 * falls dieses Attribut nicht existiert wird null zur�ckgegeben.
	 * @param name Name des Attribut das zur�ckgegeben werden soll.
	 * @return Attribute das angfragt wurde oder null.
	 */
	public TagLibTagAttr getAttribute(String name) {
		return (TagLibTagAttr)
			attributes.get(name);
	}
	


	
	/**
	 * Gibt das erste Attribut, welches innerhalb des Tag definiert wurde, zur�ck.
	 * @return  Attribut das angfragt wurde oder null.
	 */
	public TagLibTagAttr getFirstAttribute() {
		return attrFirst;
	}
	
	/**
	  * Gibt das letzte Attribut, welches innerhalb des Tag definiert wurde, zur�ck.
	 * @return Attribut das angfragt wurde oder null.
	 */
	public TagLibTagAttr getLastAttribute() {
		return attrLast;
	}

	/**
	 * Gibt den Namen des Tag zur�ck.
	 * @return String Name des Tag.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gibt den kompletten Namen des Tag zur�ck, inkl. Name-Space und Trenner.
	 * @return String Kompletter Name des Tag.
	 */
	public String getFullName() {
		String fullName;
		if(tagLib!=null)	{
			fullName=tagLib.getNameSpaceAndSeparator()+name;
		}
		else	{
			fullName=name;
		}
		return fullName;
	}

	/**
	 * Gibt die Klassendefinition, welche diesen Tag implementiert, als Zeichenkette zur�ck.
	 * Achtung: Die implementierende Klasse ist keine Java Klasse.
	 * @return String Zeichenkette der Klassendefinition.
	 */
	public String getTagClassName() {
		return tagClass;
	}
	

	public Class getClazz() throws ClassException {
		if(clazz==null) {
			clazz=ClassUtil.loadClass(tagClass);
		}
		return clazz;
	}
	
	
	public Type getTagType() throws ClassException {
		if(tagType==null) {
			tagType=Type.getType(getClazz());
		}
		return tagType;
	}
	/**
	 * @return the status (TagLib.,TagLib.STATUS_IMPLEMENTED,TagLib.STATUS_DEPRECATED,TagLib.STATUS_UNIMPLEMENTED)
	 */
	public short getStatus() {
		return status;
	}


	/**
	 * @param status the status to set (TagLib.,TagLib.STATUS_IMPLEMENTED,TagLib.STATUS_DEPRECATED,TagLib.STATUS_UNIMPLEMENTED)
	 */
	public void setStatus(short status) {
		this.status = status;
	}
	
	/**
	 * Gibt die Klassendefinition, der Klasse die den Evaluator (Translation Time Evaluator) implementiert, 
	 * als Zeichenkette zur�ck.
	 * Falls kein Evaluator definiert ist wird null zur�ckgegeben.
	 * @return String Zeichenkette der Klassendefinition.
	 */
	public String getTteClassName() {
		return tteClass;
	}
	
	public String getTttClassName() {
		return tttClass;
	}
	
	/**
	 * Gibt den Evaluator (Translation Time Evaluator) dieser Klasse zur�ck.
	 * Falls kein Evaluator definiert ist, wird null zur�ckgegeben.
	 * @return Implementation des Evaluator zu dieser Klasse.
	 * @throws EvaluatorException Falls die Evaluator-Klasse nicht geladen werden kann.
	 */
	public Evaluator getEvaluator() throws EvaluatorException {
		if(!hasTteClass()) return null;
		if(eval!=null) return eval;
		try {
			eval = (Evaluator) ClassUtil.loadInstance(tteClass);
		} 
		catch (ClassException e) {
			throw new EvaluatorException(e.getMessage());
		} 
		return eval;
	}
	
	/**
	 * Gibt den TagDependentBodyTransformer dieser Klasse zur�ck.
	 * Falls kein TagDependentBodyTransformer definiert ist, wird null zur�ckgegeben.
	 * @return Implementation des TagDependentBodyTransformer zu dieser Klasse.
	 * @throws TagLibException Falls die TagDependentBodyTransformer-Klasse nicht geladen werden kann.
	 */
	public TagDependentBodyTransformer getBodyTransformer() throws TagLibException {
		if(!hasTdbtClass()) return null;
		if(tdbt!=null) return tdbt;
		try {
			tdbt = (TagDependentBodyTransformer) ClassUtil.loadInstance(tdbtClass);
		} catch (ClassException e) {
			throw new TagLibException(e);
		} 
		return tdbt;
	}
	
	/**
	 * Gibt zur�ck ob Exception durch die implementierte Klasse abgehandelt werden oder nicht
	 * @return Wird eine Exception abgehandelt?
	 */
	public boolean handleException() {
		return handleException;
	}

	/**
	 * Gibt zur�ck, ob eine Klassendefinition
	 * der Klasse die den Evaluator (Translation Time Evaluator) implementiert existiert.
	 * @return Ob eine Evaluator definiert ist.
	 */
	public boolean hasTteClass() {
		return tteClass !=null && tteClass.length()>0;
	}

	/**
	 * Gibt zur�ck, ob eine Klassendefinition
	 * der Klasse die den TagDependentBodyTransformer implementiert existiert.
	 * @return Ob eine Evaluator definiert ist.
	 */
	public boolean hasTdbtClass() {
		return tdbtClass !=null && tdbtClass.length()>0;
	}

	/**
	 * Gibt den Attributetyp der Klasse zur�ck.
	 * ( ATTRIBUTE_TYPE_FIX, ATTRIBUTE_TYPE_DYNAMIC, ATTRIBUTE_TYPE_NONAME)
	 * @return int
	 */
	public int getAttributeType() {
		return attributeType;
	}

	/**
	 * Gibt zur�ck, ob das Tag einen Body haben kann oder nicht.
	 * @return Kann das Tag einen Body haben.
	 */
	public boolean getHasBody() {
		return hasBody;
	}

	/**
	 * Gibt die maximale Anzahl Attribute zur�ck, die das Tag haben kann.
	 * @return Maximale m�gliche Anzahl Attribute.
	 */
	public int getMax() {
		return max;
	}

	/**
	 * Gibt die minimale Anzahl Attribute zur�ck, die das Tag haben muss.
	 * @return Minimal m�gliche Anzahl Attribute.
	 */
	public int getMin() {
		return min;
	}

	/**
	 * Gibt die TagLib zur�ck zu der das Tag geh�rt.
	 * @return TagLib Zugeh�rige TagLib.
	 */
	public TagLib getTagLib() {
		return tagLib;
	}
	
	/**
	 * Gibt zur�ck ob das Tag seinen Body parsen soll oder nicht.
	 * @return Soll der Body geparst werden.
	 */
	public boolean getParseBody() {
		return parseBody;
	}
	
	/**
	 * Gibt zur�ck, ob das Tag einen Appendix besitzen kann oder nicht.
	 * @return Kann das Tag einen Appendix besitzen.
	 */
	public boolean hasAppendix() {
		return hasAppendix;
	}

	/**
	 * Fragt ab ob der Body eines Tag freiwillig ist oder nicht.
	 * @return is required
	 */
	public boolean isBodyReq() {
		return isBodyReq;
	}

	/**
	 * Fragt ab ob die verarbeitung des Inhaltes eines Tag mit einem eigenen Transformer 
	 * vorgenommen werden soll.
	 * @return Fragt ab ob die verarbeitung des Inhaltes eines Tag mit einem eigenen Transformer 
	 * vorgenommen werden soll.
	 */
	public boolean isTagDependent() {
		return isTagDependent;
	}

	/**
	 * Setzt die TagLib des Tag.
	 * Diese Methode wird durch die Klasse TagLibFactory verwendet.
	 * @param tagLib TagLib des Tag.
	 */
	protected void setTagLib(TagLib tagLib) {
		this.tagLib = tagLib;
	}
	
	/**
	 * Setzt ein einzelnes Attribut (TagLibTagAttr) eines Tag.
	 * Diese Methode wird durch die Klasse TagLibFactory verwendet.
	 * @param attribute Attribute eines Tag.
	 */
	public void setAttribute(TagLibTagAttr attribute) {
		attributes.put(attribute.getName(),attribute);
		if(attrFirst==null)attrFirst=attribute;
		attrLast=attribute;
	}

	/**
	 * Setzt den Attributtyp eines Tag.
	 * ( ATTRIBUTE_TYPE_FIX, ATTRIBUTE_TYPE_DYNAMIC, ATTRIBUTE_TYPE_FULLDYNAMIC, ATTRIBUTE_TYPE_NONAME)
	 * Diese Methode wird durch die Klasse TagLibFactory verwendet.
	 * @param attributeType The attributeType to set
	 */
	public void setAttributeType(int attributeType) {
		
		this.attributeType = attributeType;
	}

	/**
	 * Setzt die Information, was f�r ein BodyContent das Tag haben kann.
	 * Diese Methode wird durch die Klasse TagLibFactory verwendet.
	 * @param value BodyContent Information.
	 */
	public void setBodyContent(String value) {
		// empty, free, must, tagdependent
		value=value.toLowerCase().trim();
		//if(value.equals("jsp")) value="free";
		
		this.hasBody = !value.equals("empty");
		this.isBodyReq = !value.equals("free");
		this.isTagDependent = value.equals("tagdependent");
		bodyFree=value.equals("free");
	}

	/**
	 * Setzt wieviele Attribute das Tag maximal haben darf.
	 * Diese Methode wird durch die Klasse TagLibFactory verwendet.
	 * @param max The max to set
	 */
	protected void setMax(int max) {
		this.max = max;
	}

	/**
	 * Setzt wieviele Attribute das Tag minimal haben darf.
	 * Diese Methode wird durch die Klasse TagLibFactory verwendet.
	 * @param min The min to set
	 */
	protected void setMin(int min) {
		this.min = min;
	}

	/**
	 * Setzt den Namen des Tag.
	 * Diese Methode wird durch die Klasse TagLibFactory verwendet.
	 * @param name Name des Tag.
	 */
	public void setName(String name) {
		this.name = name.toLowerCase();
	}

	/**
	 * Setzt die implementierende Klassendefinition des Tag.
	 * Diese Methode wird durch die Klasse TagLibFactory verwendet.
	 * @param tagClass Klassendefinition der Tag-Implementation.
	 */
	public void setTagClass(String tagClass) {
		this.tagClass = tagClass;
	}

	/**
	 * Setzt die implementierende Klassendefinition des Evaluator.
	 * Diese Methode wird durch die Klasse TagLibFactory verwendet.
	 * @param  tteClass Klassendefinition der Evaluator-Implementation.
	 */
	protected void setTteClass(String tteClass) {
		this.tteClass = tteClass;
	}

	/**
	 * Setzt die implementierende Klassendefinition des Evaluator.
	 * Diese Methode wird durch die Klasse TagLibFactory verwendet.
	 * @param  tteClass Klassendefinition der Evaluator-Implementation.
	 */
	public void setTttClass(String tttClass) {
		this.tttClass = tttClass;
		this.tttConstructor=null;
	}

	/**
	 * Setzt die implementierende Klassendefinition des TagDependentBodyTransformer.
	 * Diese Methode wird durch die Klasse TagLibFactory verwendet.
	 * @param  tdbtClass Klassendefinition der TagDependentBodyTransformer-Implementation.
	 */
	public void setTdbtClass(String tdbtClass) {
		this.tdbtClass = tdbtClass;
		this.tdbt = null;
	}

	/**
	 * Setzt, ob der Body des Tag geparst werden soll oder nicht.
	 * Diese Methode wird durch die Klasse TagLibFactory verwendet.
	 * @param parseBody Soll der Body geparst werden.
	 */
	public void setParseBody(boolean parseBody) {
		this.parseBody = parseBody;
	}

	/**
	 * Setzt ob das Tag einen Appendix besitzen kann oder nicht.
	 * Diese Methode wird durch die Klasse TagLibFactory verwendet.
	 * @param hasAppendix Kann das Tag einen Appendix besitzen.
	 */
	public void setAppendix(boolean hasAppendix) {
		this.hasAppendix = hasAppendix;
	}
	/**
	 * @return Returns the description.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description The description to set.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return Returns the bodyIsFree.
	 */
	public boolean isBodyFree() {
		return bodyFree;
	}

	public boolean hasBodyMethodExists() {
		Class clazz= ClassUtil.loadClass(getTagClassName(),(Class)null);//Class.orName(tag.getTagClassName());
		if(clazz==null) return false;
		
		try {
			java.lang.reflect.Method method = clazz.getMethod("hasBody", new Class[]{boolean.class});
			if(method==null)return false;
			return method.getReturnType()==void.class;
		} 
		catch (Exception e) {}
		return false;
	}

	/**
	 * @return Gibt zur�ck ob ein Attribut Evaluator definiert ist oder nicht.
	 */
	public boolean hasAttributeEvaluator() {
		return strAttributeEvaluator!=null;
	}

	/**
	 * @return Gibt den AttributeEvaluator zum Tag zur�ck
	 * @throws AttributeEvaluatorException
	 */
	public AttributeEvaluator getAttributeEvaluator() throws AttributeEvaluatorException {
		if(!hasAttributeEvaluator()) return null;
		if(attributeEvaluator!=null) return attributeEvaluator;
		try {
			return  attributeEvaluator=(AttributeEvaluator) ClassUtil.loadInstance(strAttributeEvaluator);
			
		} catch (ClassException e) {
			throw new AttributeEvaluatorException(e.getMessage());
		} 
	}

	/**
	 * Setzt den Namen der Klasse welche einen AttributeEvaluator implementiert.
	 * @param value Name der AttributeEvaluator Klassse
	 */
	public void setAttributeEvaluatorClassName(String value) {
		strAttributeEvaluator=value;
		
	}

	/**
	 * sets if tag handle exception inside his body or not
	 * @param handleException handle it or not
	 */
	public void setHandleExceptions(boolean handleException) {
		this.handleException=handleException;
	}

    /**
     * @return
     */
    public boolean hasDefaultValue() {
        return hasDefaultValue;
    }

    /**
     * @param hasDefaultValue The hasDefaultValue to set.
     */
    public void setHasDefaultValue(boolean hasDefaultValue) {
        this.hasDefaultValue = hasDefaultValue;
    }

	/**
	 * return ASM Tag for this tag
	 * @param line
	 * @return

	 */
	public Tag getTag(int startline,int endline) throws TagLibException {
		if(StringUtil.isEmpty(tttClass)) return new TagBase(startline,endline);
		try {
			return _getTag(startline,endline);
		} 
		catch (ClassException e) {
			throw new TagLibException(e.getMessage());
		} 
		catch (NoSuchMethodException e) {
			throw new TagLibException(e.getMessage());
		} 
		catch (Throwable e) {
			throw new TagLibException(e);
		}
	}
	private Tag _getTag(int startline,int endline) throws ClassException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		if(tttConstructor==null) {
			Class clazz = ClassUtil.loadClass(tttClass);
			tttConstructor = clazz.getConstructor(CONSTRUCTOR_PARAMS);
		}
		return (Tag) tttConstructor.newInstance(new Object[]{Integer.valueOf(startline),Integer.valueOf(endline)});
	}

	public void setAllowRemovingLiteral(boolean allowRemovingLiteral) {
		this.allowRemovingLiteral=allowRemovingLiteral;
	}

	/**
	 * @return the allowRemovingLiteral
	 */
	public boolean isAllowRemovingLiteral() {
		return allowRemovingLiteral;
	}

	public String getAttributeNames() {
		Iterator<String> it = attributes.keySet().iterator();
		StringBuffer sb=new StringBuffer();
		while(it.hasNext()) {
			if(sb.length()>0)sb.append(",");
			sb.append(it.next());
		}
		return sb.toString();
	}

	public String getSetter(Attribute attr, Type type) {
		if(tagLib.isCore())
			return "set"+StringUtil.ucFirst(attr.getName());
		
		String setter=(String) setters.get(attr.getName());
		if(setter!=null)return setter;
		setter = "set"+StringUtil.ucFirst(attr.getName());
		Class clazz;
		try {
			if(type==null) type = Cast.getType(attr.getType());
			clazz=ClassUtil.loadClass(getTagClassName());
			java.lang.reflect.Method m = ClassUtil.getMethodIgnoreCase(clazz,setter,new Class[]{ClassUtil.loadClass(type.getClassName())});
			setter=m.getName();
		} 
		catch (Exception e) {
			//print.err(setter);
			e.printStackTrace();
		}
		setters.put(attr.getName(), setter);
		return setter;
	}

	public String getHash() {
		StringBuffer sb=new StringBuffer();
		sb.append(this.getTagClassName());
		sb.append(this.getAttributeNames());
		sb.append(this.getAttributeType());
		sb.append(this.getMax());
		sb.append(this.getMin());
		sb.append(this.getName());
		sb.append(this.getParseBody());
		sb.append(this.getTteClassName());
		sb.append(this.getTttClassName());
		Iterator it = this.getAttributes().entrySet().iterator();
		Map.Entry entry;
		while(it.hasNext()){
			entry=(Entry) it.next();
			sb.append(entry.getKey());
			sb.append(((TagLibTagAttr)entry.getValue()).getHash());
		}
		
		try {
			return Md5.getDigestAsString(sb.toString());
		} catch (IOException e) {
			return "";
		}
	}

	public TagLibTagAttr getDefaultAttribute() {
		return defaultAttribute;
	}
	
	public void setDefaultAttribute(TagLibTagAttr defaultAttribute) {
		this.defaultAttribute=defaultAttribute;
	}


	public void setScript(TagLibTagScript script) {
		this.script=script;
	}


	/**
	 * @return the script
	 */
	public TagLibTagScript getScript() {
		return script;
	}

}