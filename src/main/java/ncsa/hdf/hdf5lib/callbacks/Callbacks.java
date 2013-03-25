package ncsa.hdf.hdf5lib.callbacks;

/** All callback definitions must derive from this interface.  Any 
 * derived interfaces must define a single public method  named "callback".
 * You are responsible for deregistering your callback (if necessary)
 * in its {@link Object#finalize} method.  If native code attempts to call
 * a callback which has been GC'd, you will likely crash the VM.  If 
 * there is no method to deregister the callback (e.g. <code>atexit</code>
 * in the C library), you must ensure that you always keep a live reference
 * to the callback object.<p>
 * A callback should generally never throw an exception, since it doesn't
 * necessarily have an encompassing Java environment to catch it.  Any
 * exceptions thrown will be passed to the default callback exception
 * handler. 
 */
public interface Callbacks {

}
