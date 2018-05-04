package com.template.api

import com.template.contract.IOUContract
import com.template.flows.ExampleFlow.Initiator
import com.template.model.IOU
import com.template.state.IOUState
import net.corda.client.rpc.notUsed
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.DEFAULT_PAGE_NUM
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import khttp.put
import org.json.JSONObject
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import java.text.SimpleDateFormat
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Created by acesuser on 4/15/2018.
 */
// *****************
// * API Endpoints *
// *****************

val SERVICE_NAMES = listOf("Notary", "Network Map Service")

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("example")
class ExampleApi(private val rpcOps: CordaRPCOps) {
    private val myLegalName: CordaX500Name = rpcOps.nodeInfo().legalIdentities.first().name

    companion object {
        private val logger: Logger = loggerFor<ExampleApi>()
        private val subscription : MutableMap<String,MutableSet<String>> = mutableMapOf()
        private val subscribed :  MutableSet<String>  = mutableSetOf()
    }

    @POST
    @Path("me")
    fun login(@QueryParam("userid") username:String, @QueryParam("password")  pwd :String) {
   }
    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = rpcOps.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }

    /**
     * Displays all IOU states that exist in the node's vault.
     */


    @GET
    @Path("ious")
    @Produces(MediaType.APPLICATION_JSON)
            // Filter by state type: IOU.
    fun getIOUs(): List<StateAndRef<ContractState>> {

        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        var pageNumber = DEFAULT_PAGE_NUM
        val states = mutableListOf<StateAndRef<ContractState>>()
        do {
            val pageSpec = PageSpecification(pageSize = 200, pageNumber = pageNumber)
            val results = rpcOps.vaultQueryByWithPagingSpec(IOUState::class.java,generalCriteria, pageSpec)
            states.addAll(results.states);
            pageNumber++
        } while ((pageSpec.pageSize * pageSpec.pageNumber) <= results.totalStatesAvailable)

        return states;

    }

    data class SubscribeRequest(val stock:String , val bank : String?) {}
    @PUT
    @Path("subscribe")
    fun subscribe(  req: SubscribeRequest  ): Response {
//        val api = HttpApi.fromHostAndPort(HostAndPort.fromString(  "localhost:10004" ) , "api/example")
//        api.postJson("accpetSubscribe", iou)
//        api.post
        //TODO: should use authenticated  RPC API
        val resp =  put("http://localhost:10007/api/example/accpetSubscribe" , /*params = mapOf( "stock" to  req.stock ) */
                json = JSONObject(SubscribeRequest(req.stock, myLegalName.organisation)))

        subscribed.add(req.stock)
        return Response.status(resp.statusCode ).entity(resp.text).build()
    }

    @PUT
    @Path("accpetSubscribe")
    fun accpetSubscribe( req: SubscribeRequest   ): Response {
        if( req.bank == null){
            return Response.status(Response.Status.BAD_REQUEST ).entity("bank can't be null").build()
        }
        val list : MutableSet<String>  = subscription.getOrPut( req.stock  ) {  mutableSetOf<String>() }

        list.add( req.bank)

        return Response.status(Response.Status.OK ).entity("Subscribed").build()
    }

    @GET
    @Path("subscribers")
    fun subscribers(  @QueryParam("stock") stock:String   ): Response {
        return Response.status(Response.Status.OK ).entity(
                subscription.getOrElse(stock){ mutableSetOf<String>()}
        ).build()
    }
    @GET
    @Path("subscribed")
    fun subscribed(  ): Response {
        return Response.status(Response.Status.OK ).entity(
                subscribed
        ).build()
    }

    /**
     * Initiates a flow to agree an IOU between two parties.
     *
     * Once the flow finishes it will have written the IOU to ledger. Both the lender and the borrower will be able to
     * see it when calling /api/example/ious on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    @PUT
    @Path("{party}/create-iou")
    fun createIOU(iou: IOU, @PathParam("party") partyName: String): Response {
        val partyPK = CordaX500Name(partyName,"London","GB");
        val otherParty= rpcOps.wellKnownPartyFromX500Name(partyPK)  ?: throw IllegalArgumentException("Unknown party name.")
        if (otherParty == null) {
            return Response.status(Response.Status.BAD_REQUEST).build()
        }

        val state = IOUState(
                iou,
                rpcOps.nodeInfo().legalIdentities.first(),
                otherParty)

        val (status, msg) = try {
            val flowHandle = rpcOps.startTrackedFlow(::Initiator, state, otherParty)
            flowHandle.progress.subscribe { println(">> $it") }

            // The line below blocks and waits for the future to resolve.
            val result = flowHandle.returnValue.getOrThrow()

            Response.Status.CREATED to "Transaction id ${result.id} committed to ledger."

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.Status.BAD_REQUEST to "Transaction failed."
        }

        return Response.status(status).entity(msg).build()
    }

//    @PUT
//    @Path("create-iou")
//    fun createIOU(@QueryParam("iouValue") iouValue: Int, @QueryParam("partyName") partyName: CordaX500Name?): Response {
//        if (iouValue <= 0 ) {
//            return Response.status(Response.Status.BAD_REQUEST).entity("Query parameter 'iouValue' must be non-negative.\n").build()
//        }
//        if (partyName == null) {
//            return Response.status(Response.Status.BAD_REQUEST).entity("Query parameter 'partyName' missing or has wrong format.\n").build()
//        }
//        val otherParty = rpcOps.wellKnownPartyFromX500Name(partyName) ?:
//                return Response.status(Response.Status.BAD_REQUEST).entity("Party named $partyName cannot be found.\n").build()
//
//        return try {
//            val signedTx = rpcOps.startTrackedFlow(::Initiator, iouValue, otherParty).returnValue.getOrThrow()
//            Response.status(Response.Status.CREATED).entity("Transaction id ${signedTx.id} committed to ledger.\n").build()
//
//        } catch (ex: Throwable) {
//            logger.error(ex.message, ex)
//            Response.status(Response.Status.BAD_REQUEST).entity(ex.message!!).build()
//        }
//    }
@PUT
@Path("{party}/updateWithScramble")
fun updateWithScramble(iou: IOU, @PathParam("party") partyName: String): Response {
    val partyPK = CordaX500Name(partyName,"London","GB");
    val otherParty= rpcOps.wellKnownPartyFromX500Name(partyPK)  ?: throw IllegalArgumentException("Unknown party name.")
    if (otherParty == null) {
        return Response.status(Response.Status.BAD_REQUEST).build()
    }

    val sdf = SimpleDateFormat("yyyyMMdd HH:mm:ss", Locale.getDefault());
    val now = Date();
    var copied = iou.copy(payDate = sdf.format(now));

//        logger.error("Party:" + partyName);
//        if (partyName.equals("BankA")) {
//            copied = iou.copy(stock = "BankA-AABB");
//        } else if (partyName.equals("BankB")) {
//            copied = iou.copy(stock = "BankB-CCDD");
//        } else if ((partyName.equals("BankC"))) {
//            copied = iou.copy(stock = "BankC-EEFF");
//        }

    val state = IOUState(
            copied,
            rpcOps.nodeInfo().legalIdentities.first(),
            otherParty)

    val (status, msg) = try {
        val flowHandle = rpcOps
                .startTrackedFlow(::Initiator, state, otherParty)
        flowHandle.progress.subscribe { println(">> $it") }

        // The line below blocks and waits for the future to resolve.
        val result = flowHandle
                .returnValue
                .getOrThrow()

        Response.Status.CREATED to "Transaction id ${result.id} committed to ledger."

    } catch (ex: Throwable) {
        logger.error(ex.message, ex)
        Response.Status.BAD_REQUEST to "Transaction failed."
    }

    return Response.status(status).entity(msg).build()
    }
}

