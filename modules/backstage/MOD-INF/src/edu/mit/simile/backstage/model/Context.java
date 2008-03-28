package edu.mit.simile.backstage.model;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.mozilla.javascript.Scriptable;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.sail.SailRepositoryConnection;

import edu.mit.simile.backstage.model.data.Database;
import edu.mit.simile.backstage.model.ui.lens.Lens;
import edu.mit.simile.backstage.model.ui.lens.LensRegistry;
import edu.mit.simile.backstage.util.DefaultScriptableObject;
import edu.mit.simile.backstage.util.SailUtilities;
import edu.mit.simile.backstage.util.Utilities;


public class Context {
    private static Logger _logger = Logger.getLogger(Context.class);
	
    protected Context _parent;
    protected Exhibit _exhibit;
    protected Properties _properties = new Properties();
    protected LensRegistry	_lensRegistry;
    
    public Context(Context parent) {
        _parent = parent;
        _exhibit = parent.getExhibit();
        _lensRegistry = new LensRegistry(parent != null ? parent._lensRegistry : null);
    }
    
    public Context(Exhibit exhibit) {
        _parent = null;
        _exhibit = exhibit;
        _lensRegistry = new LensRegistry(null);
    }
    
    public void dispose() {
        _parent = null;
        _exhibit = null;
        _properties.clear();
        _properties = null;
        _lensRegistry = null;
    }
    
    public Exhibit getExhibit() {
        return _exhibit;
    }
    
    public Database getDatabase() {
        return _exhibit.getDatabase();
    }
    
    public Object getProperty(String name) {
        Object o = _properties.getProperty(name);
        if (o == null && _parent != null) {
            o = _parent.getProperty(name);
        }
        return o;
    }
    
    public String getStringProperty(String name) {
        return (String) getProperty(name);
    }
    
    public void setProperty(String name, Object value) {
        _properties.put(name, value);
    }
    
    public void configure(Scriptable config, BackChannel backChannel) {
    	String id = Utilities.getString(config, "id");
    	if (id != null) {
    		_exhibit.setContext(id, this);
    	}
    	
    	Object o = config.get("lensRegistry", config);
    	if (o != null) {
    		_lensRegistry.configure((Scriptable) o, backChannel);
    	}
    }
    
    public Scriptable generateLens(String itemID) {
        DefaultScriptableObject result = new DefaultScriptableObject();
        result.put("itemID", result, itemID);
        
        Database database = getDatabase();
        
        URI itemURI = database.getItemURI(itemID);
        String typeId = "Item";
        try {
            SailRepositoryConnection connection = (SailRepositoryConnection)
                database.getRepository().getConnection();
            
            try {
            	Value type = SailUtilities.getObject(connection, itemURI, RDF.TYPE);
            	if (type instanceof URI) {
            		typeId = database.getTypeId((URI) type);
            	}
            	
            	Lens lens = _lensRegistry.getLens(typeId);
            	
            	lens.render(itemURI, result, database, connection);
            } finally {
                connection.close();
            }
        } catch (Exception e) {
            _logger.error("Error generating lens for " + itemID, e);
            result.put("error", result, e.toString());
        }
        
        result.put("itemType", result, typeId);
        
        return result;
    }
}
