// **********************************************************************
//
// Copyright (c) 2001
// MutableRealms, Inc.
// Huntsville, AL, USA
//
// All Rights Reserved
//
// **********************************************************************

package IceInternal;

public class Incoming
{
    public
    Incoming(Instance instance, Ice.ObjectAdapter adapter)
    {
        _adapter = adapter;
        _is = new BasicStream(instance);
        _os = new BasicStream(instance);
    }

    public void
    invoke(boolean response)
    {
        Ice.Current current = new Ice.Current();
        current.identity = new Ice.Identity();
        current.identity.__read(_is);
        current.facet = _is.readString();
        current.operation = _is.readString();
        current.nonmutating = _is.readBool();
        int sz = _is.readSize();
        while (sz-- > 0)
        {
            String first = _is.readString();
            String second = _is.readString();
            if (current.context == null)
            {
                current.context = new java.util.HashMap();
            }
            current.context.put(first, second);
        }

        int statusPos = 0;
        if (response)
        {
            statusPos = _os.size();
            _os.writeByte((byte)0);
        }

        //
        // Input and output parameters are always sent in an
        // encapsulation, which makes it possible to forward requests as
        // blobs.
        //
        _is.startReadEncaps();
        if (response)
        {
            _os.startWriteEncaps();
        }

        Ice.Object servant = null;
        Ice.ServantLocator locator = null;
        Ice.LocalObjectHolder cookie = new Ice.LocalObjectHolder();

        try
        {
            if (_adapter != null)
            {
                servant = _adapter.identityToServant(current.identity);

                if (servant == null && current.identity.category.length() > 0)
                {
                    locator = _adapter.findServantLocator(current.identity.category);
                    if (locator != null)
                    {
                        servant = locator.locate(_adapter, current, cookie);
                    }
                }

                if (servant == null)
                {
                    locator = _adapter.findServantLocator("");
                    if (locator != null)
                    {
                        servant = locator.locate(_adapter, current, cookie);
                    }
                }
            }

            DispatchStatus status;

            if (servant == null)
            {
                status = DispatchStatus.DispatchObjectNotExist;
            }
            else
            {
                if (current.facet.length() > 0)
                {
                    Ice.Object facetServant = servant.ice_findFacet(current.facet);
                    if (facetServant == null)
                    {
                        status = DispatchStatus.DispatchFacetNotExist;
                    }
                    else
                    {
                        status = facetServant.__dispatch(this, current);
                    }
                }
                else
                {
                    status = servant.__dispatch(this, current);
                }
            }

            if (locator != null && servant != null)
            {
                assert(_adapter != null);
                locator.finished(_adapter, current, servant, cookie.value);
            }

            _is.endReadEncaps();
            if (response)
            {
                _os.endWriteEncaps();

                if (status != DispatchStatus.DispatchOK && status != DispatchStatus.DispatchUserException)
                {
                    _os.resize(statusPos, false);
                    _os.writeByte((byte)status.value());
                }
                else
                {
                    int save = _os.pos();
                    _os.pos(statusPos);
                    _os.writeByte((byte)status.value());
                    _os.pos(save);
                }
            }
        }
        catch (Ice.LocationForward ex)
        {
            if (locator != null && servant != null)
            {
                assert(_adapter != null);
                locator.finished(_adapter, current, servant, cookie.value);
            }

            _is.endReadEncaps();
            if (response)
            {
                _os.endWriteEncaps();
                _os.resize(statusPos, false);
                _os.writeByte((byte)DispatchStatus._DispatchLocationForward);
                _os.writeProxy(ex._prx);
            }
        }
        catch (Ice.ObjectNotExistException ex)
        {
            if (locator != null && servant != null)
            {
                assert(_adapter != null);
                locator.finished(_adapter, current, servant, cookie.value);
            }

            _is.endReadEncaps();
            if (response)
            {
                _os.endWriteEncaps();
                _os.resize(statusPos, false);
                _os.writeByte((byte)DispatchStatus._DispatchObjectNotExist);
            }
        }
        catch (Ice.FacetNotExistException ex)
        {
            if (locator != null && servant != null)
            {
                assert(_adapter != null);
                locator.finished(_adapter, current, servant, cookie.value);
            }

            _is.endReadEncaps();
            if (response)
            {
                _os.endWriteEncaps();
                _os.resize(statusPos, false);
                _os.writeByte((byte)DispatchStatus._DispatchFacetNotExist);
            }
        }
        catch (Ice.OperationNotExistException ex)
        {
            if (locator != null && servant != null)
            {
                assert(_adapter != null);
                locator.finished(_adapter, current, servant, cookie.value);
            }

            _is.endReadEncaps();
            if (response)
            {
                _os.endWriteEncaps();
                _os.resize(statusPos, false);
                _os.writeByte((byte)DispatchStatus._DispatchOperationNotExist);
            }
        }
        catch (Ice.LocalException ex)
        {
            if (locator != null && servant != null)
            {
                assert(_adapter != null);
                locator.finished(_adapter, current, servant, cookie.value);
            }

            _is.endReadEncaps();
            if (response)
            {
                _os.endWriteEncaps();
                _os.resize(statusPos, false);
                _os.writeByte(
                    (byte)DispatchStatus._DispatchUnknownLocalException);
            }

            throw ex;
        }
        /* Not possible in Java - UserExceptions are checked exceptions
        catch (Ice.UserException ex)
        {
            if (locator != null && servant != null)
            {
                assert(_adapter != null);
                locator.finished(_adapter, current, servant, cookie.value);
            }

            _is.endReadEncaps();
            if (response)
            {
                _os.endWriteEncaps();
                _os.resize(statusPos, false);
                _os.writeByte(
                    (byte)DispatchStatus._DispatchUnknownUserException);
            }

            throw ex;
        }
        */
        catch (RuntimeException ex)
        {
            if (locator != null && servant != null)
            {
                assert(_adapter != null);
                locator.finished(_adapter, current, servant, cookie.value);
            }

            _is.endReadEncaps();
            if (response)
            {
                _os.endWriteEncaps();
                _os.resize(statusPos, false);
                _os.writeByte((byte)DispatchStatus._DispatchUnknownException);
            }

            throw ex;
        }
    }

    public BasicStream
    is()
    {
        return _is;
    }

    public BasicStream
    os()
    {
        return _os;
    }

    public void
    destroy()
    {
        _is.destroy();
        _os.destroy();
    }

    private Ice.ObjectAdapter _adapter;
    private BasicStream _is;
    private BasicStream _os;
}
