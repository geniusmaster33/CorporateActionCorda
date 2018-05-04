package com.template.state

/**
 * Created by acesuser on 4/15/2018.
 */
import com.template.contract.IOUContract
import com.template.model.IOU
import com.template.schema.IOUSchemaV1
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.keys
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.security.PublicKey



/**
 * The state object recording IOU agreements between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 * @param iou details of the IOU.
 * @param sender the party issuing the IOU.
 * @param recipient the party receiving and approving the IOU.
 * @param contract the contract which governs which transactions are valid for this state object.
 */
data class IOUState(val iou: IOU,
                    val sender: Party,
                    val recipient: Party,
                    override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {

    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(sender, recipient)

    /** Tells the vault to track a state if we are one of the parties involved. */
   // override fun isRelevant(ourKeys: Set<PublicKey>) = ourKeys.intersect(participants.flatMap { it.keys }).isNotEmpty()

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is IOUSchemaV1 -> IOUSchemaV1.PersistentIOU(
                    senderName = this.sender.name.toString(),
                    recipientName = this.recipient.name.toString(),
                    value = this.iou.value
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(IOUSchemaV1)
}



