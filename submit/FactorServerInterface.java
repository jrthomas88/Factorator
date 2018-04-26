package model;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface FactorServerInterface extends Remote {
    public FactorData getData() throws RemoteException;
}
